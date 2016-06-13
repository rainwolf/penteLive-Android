
#include "AiWrapper.h"
#include "Ai.h"

/*
 * Class:     be_submanifold_pentelive_Ai
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT jlong JNICALL Java_be_submanifold_pentelive_Ai_init
  (JNIEnv *env, jobject o, jintArray atbl, jintArray asrc, jint s)
{
    CAi *cai = new CAi();

    jint *tbl = (*env).GetIntArrayElements(atbl, 0);
    jint *src = (*env).GetIntArrayElements(asrc, 0);
	cai->Init(tbl, src, s);

    return ((long)cai);
}

/*
 * Class:     be_submanifold_pentelive_Ai
 * Method:    destroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_be_submanifold_pentelive_Ai_privateDestroy(JNIEnv *env, jobject o, jlong ptr)
{
	CAi* cai=(CAi*)ptr;
    cai->CAi::~CAi();
    //printf("c destroyed\n");
}

/*
 * Class:     be_submanifold_pentelive_Ai
 * Method:    stop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_be_submanifold_pentelive_Ai_stop(JNIEnv *env, jobject o, jlong ptr)
{
   CAi* cai=(CAi*)ptr;
   cai->stopped = 1;
}

/*
 * Class:     be_submanifold_pentelive_Ai
 * Method:    stop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_be_submanifold_pentelive_Ai_toggleCallbacks(JNIEnv *env, jobject o, jlong ptr, jint callbacks)
{
   CAi* cai=(CAi*)ptr;
   cai->callbacks = callbacks;
}

/*
 * Class:     be_submanifold_pentelive_Ai
 * Method:    stop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_be_submanifold_pentelive_Ai_start(JNIEnv *env, jobject o, jlong ptr)
{
   CAi* cai=(CAi*)ptr;
   cai->stopped = 0;
}

/*
 * Class:     be_submanifold_pentelive_Ai
 * Method:    move
 * Signature: ([I)I
 */
JNIEXPORT jint JNICALL Java_be_submanifold_pentelive_Ai_move(JNIEnv *env, jobject obj, 
	jlong ptr, jintArray movesArr, jint game, jint level, jint vct)
{
    CAi* cai=(CAi*)ptr;
    if (cai->stopped) return -1;

    jclass cls = (*env).GetObjectClass(obj);
    jmethodID mid = 
         (*env).GetMethodID(cls, "aiEvaluatedCallBack", "()V");
    if (mid == 0) {
        //printf("method 1 not found\n");
        return -1; /* method not found */
    }
    jmethodID mid2 = 
         (*env).GetMethodID(cls, "aiVisualizationCallBack", "([I)V");
    if (mid2 == 0) {
        //printf("method 2 not found\n");
        return -1; /* method not found */
    }
    
    if (game == 3) {
        cai->Kgame = 1;
    }
    else {
        cai->Kgame = 0;
    }

    jsize numMoves = (*env).GetArrayLength(movesArr);
    jint *movesp = (*env).GetIntArrayElements(movesArr, 0);
    cai->Move(env, obj, mid, mid2, numMoves, movesp, level, vct);
    
    (*env).ReleaseIntArrayElements(movesArr, movesp, 0);
    
    if (cai->stopped) return -1;
    else return cai->bmove;
}
