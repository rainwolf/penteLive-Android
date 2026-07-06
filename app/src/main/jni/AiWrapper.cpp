// JNI wrapper around the canonical mmai engine (CAi, synced from
// react_mmai/MMAIWASM). Preserves the old Java-visible contract of
// be.submanifold.pentelive.Ai while adapting to the engine's mobile
// portability seam (ctor filesDir, requestStop, setListener, setCallbackMask).
//
// Design:
//   * One persistent CAi lives across many moves (getMove() self-resets, so
//     replaying a growing move list on the same instance is safe and avoids
//     reloading pente.tbl/pente.scs/opngbk.pen every move).
//   * The engine is (re)built only when the (game, level) pair changes.
//   * A wrapper-local `stopped` flag reproduces the old -1 "cancelled"
//     sentinel exactly: move() returns -1 whenever it is set, whether the
//     stop arrived before or during the search.

#include "AiWrapper.h"
#include "Ai.h"
#include <string>
#include <cstdint>
#include <atomic>

// --- JNI callback bridge -----------------------------------------------------
// Mirrors the old wrapper's two callbacks. env/obj are bound only for the
// duration of a move() call, because the engine fires these on the calling
// (AI) thread during getMove(); off-thread use of a JNIEnv* is illegal.
struct JniAiListener : public CAiListener {
    JNIEnv  *env = nullptr;
    jobject  obj = nullptr;
    jmethodID evalMid = 0;   // aiEvaluatedCallBack ()V
    jmethodID visMid  = 0;   // aiVisualizationCallBack ([I)V
    // Set once a Java callback leaves a pending exception. Calling further JNI
    // functions (other than a small "safe" set) with an exception pending is
    // undefined behaviour, and the engine keeps firing callbacks for the rest of
    // the search on this thread, so we must stop touching JNI. We deliberately do
    // NOT ExceptionClear(): the pending exception is left in place so that when
    // move() returns to Java the JVM re-raises it at the call site (AIRunnable.run
    // already catches Throwable). The only JNI calls move() makes after this point
    // -- ReleaseIntArrayElements / clearing the local refs -- are on JNI's list of
    // functions that are safe to invoke with an exception pending.
    bool suppressed = false;

    void aiEvaluated() override {
        // Old engine fired this UNCONDITIONALLY (not gated by the mask).
        if (suppressed || !env || !obj || !evalMid) return;
        env->CallVoidMethod(obj, evalMid);
        if (env->ExceptionCheck()) suppressed = true;
    }

    void aiVisualization(const int *data, int len) override {
        // Mask-gated by the engine (callbackMask); we just marshal. A fresh
        // jintArray per call mirrors the old behaviour (len is always 361).
        if (suppressed || !env || !obj || !visMid || !data || len <= 0) return;
        jintArray arr = env->NewIntArray(len);
        if (!arr) { suppressed = true; return; } // OOM etc. -> exception pending
        env->SetIntArrayRegion(arr, 0, len, reinterpret_cast<const jint *>(data));
        env->CallVoidMethod(obj, visMid, arr);
        if (env->ExceptionCheck()) { suppressed = true; env->DeleteLocalRef(arr); return; }
        env->DeleteLocalRef(arr);
    }
};

// --- persistent per-Ai state -------------------------------------------------
struct AiHolder {
    CAi          *cai = nullptr;
    std::string   filesDir;
    int               game  = -1;   // cached; engine rebuilt when this changes
    int               level = -1;   // cached; engine rebuilt when this changes
    std::atomic<int>  stopped{0};   // wrapper-local cancelled sentinel (old cai->stopped);
                                    // written by stop() on the UI thread, read by move()
                                    // on the AI thread, so it must be atomic for visibility
    int               callbackMask = 0; // old cai->callbacks (visualization gate)
    JniAiListener listener;
};

static inline AiHolder *holderOf(jlong ptr) {
    return reinterpret_cast<AiHolder *>(static_cast<intptr_t>(ptr));
}

extern "C" {

JNIEXPORT jlong JNICALL Java_be_submanifold_pentelive_Ai_init
  (JNIEnv *env, jobject /*o*/, jstring jfilesDir, jint /*size*/)
{
    // size is legacy (the engine board is a fixed 19x19); accepted for contract
    // compatibility but unused. The CAi itself is built lazily in the first
    // move() once (game, level) are known.
    AiHolder *h = new AiHolder();
    if (jfilesDir) {
        const char *c = env->GetStringUTFChars(jfilesDir, nullptr);
        if (c) { h->filesDir = c; env->ReleaseStringUTFChars(jfilesDir, c); }
    }
    return static_cast<jlong>(reinterpret_cast<intptr_t>(h));
}

JNIEXPORT void JNICALL Java_be_submanifold_pentelive_Ai_privateDestroy
  (JNIEnv * /*env*/, jobject /*o*/, jlong ptr)
{
    AiHolder *h = holderOf(ptr);
    if (!h) return;
    delete h->cai;   // proper delete (old code called ~CAi() explicitly -> leak)
    delete h;
}

JNIEXPORT void JNICALL Java_be_submanifold_pentelive_Ai_stop
  (JNIEnv * /*env*/, jobject /*o*/, jlong ptr)
{
    AiHolder *h = holderOf(ptr);
    if (!h) return;
    h->stopped = 1;                       // wrapper sentinel -> move() returns -1
    if (h->cai) h->cai->requestStop();    // async: bail the running search
}

JNIEXPORT void JNICALL Java_be_submanifold_pentelive_Ai_start
  (JNIEnv * /*env*/, jobject /*o*/, jlong ptr)
{
    AiHolder *h = holderOf(ptr);
    if (!h) return;
    h->stopped = 0;                       // clear the cancelled sentinel
}

JNIEXPORT void JNICALL Java_be_submanifold_pentelive_Ai_toggleCallbacks
  (JNIEnv * /*env*/, jobject /*o*/, jlong ptr, jint callbacks)
{
    AiHolder *h = holderOf(ptr);
    if (!h) return;
    h->callbackMask = callbacks;
    if (h->cai) h->cai->setCallbackMask(callbacks);
}

JNIEXPORT jint JNICALL Java_be_submanifold_pentelive_Ai_move
  (JNIEnv *env, jobject obj, jlong ptr, jintArray movesArr,
   jint game, jint level, jint /*vct*/)
{
    // vct: the canonical engine manages verified-connect-threat search
    // internally; the old per-call vct knob no longer exists, so it is
    // accepted and ignored.
    AiHolder *h = holderOf(ptr);
    if (!h) return -1;
    if (h->stopped) return -1;            // cancelled before search -> -1 sentinel

    // (Re)build the persistent engine only when game/level change. game IDs are
    // canonical (1 Pente, 3 Keryo, 11 Poof, 13 Connect6, 15 Boat, 25 O-Pente);
    // configFor() maps them (and Speed twins) internally.
    if (h->cai == nullptr || h->game != game || h->level != level) {
        delete h->cai;
        h->cai = new CAi(game, level, true, h->filesDir.c_str());
        h->game = game;
        h->level = level;
        h->cai->setCallbackMask(h->callbackMask);
        h->cai->setListener(&h->listener);
    }

    // Resolve the two Java callbacks (names/signatures preserved from old wrapper).
    jclass cls = env->GetObjectClass(obj);
    jmethodID evalMid = env->GetMethodID(cls, "aiEvaluatedCallBack", "()V");
    if (evalMid == 0) return -1;          // method not found
    jmethodID visMid = env->GetMethodID(cls, "aiVisualizationCallBack", "([I)V");
    if (visMid == 0) return -1;           // method not found

    // Bind env/obj for callbacks that fire on THIS thread during getMove().
    h->listener.env = env;
    h->listener.obj = obj;
    h->listener.evalMid = evalMid;
    h->listener.visMid = visMid;
    h->listener.suppressed = false; // reset per search; a callback may re-set it

    jsize numMoves = env->GetArrayLength(movesArr);
    jint *movesp = env->GetIntArrayElements(movesArr, nullptr);
    int result = h->cai->getMove(reinterpret_cast<int *>(movesp),
                                 static_cast<int>(numMoves));
    env->ReleaseIntArrayElements(movesArr, movesp, JNI_ABORT); // input-only, no copy-back

    // Unbind so no stale env/obj is ever touched off-thread.
    h->listener.env = nullptr;
    h->listener.obj = nullptr;

    // A stopped search still returns a legal move, but the wrapper must report
    // the old -1 cancelled sentinel.
    if (h->stopped) return -1;
    return static_cast<jint>(result);
}

} // extern "C"
