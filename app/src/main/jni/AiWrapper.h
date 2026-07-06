/* JNI surface for be.submanifold.pentelive.Ai.
 * Hand-maintained (was machine generated). The C++ side now holds a persistent
 * engine instance behind an opaque jlong handle; see AiWrapper.cpp. */
#include <jni.h>

/* Header for class be_submanifold_pentelive_Ai */

#ifndef _Included_be_submanifold_pentelive_Ai
#define _Included_be_submanifold_pentelive_Ai
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     be_submanifold_pentelive_Ai
 * Method:    init
 * Signature: (Ljava/lang/String;I)J
 * filesDir: directory holding pente.tbl / pente.scs / opngbk.pen.
 */
JNIEXPORT jlong JNICALL Java_be_submanifold_pentelive_Ai_init
  (JNIEnv *, jobject, jstring, jint);

/*
 * Class:     be_submanifold_pentelive_Ai
 * Method:    privateDestroy
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_be_submanifold_pentelive_Ai_privateDestroy
  (JNIEnv *, jobject, jlong);

/*
 * Class:     be_submanifold_pentelive_Ai
 * Method:    stop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_be_submanifold_pentelive_Ai_stop
  (JNIEnv *, jobject, jlong);

/*
 * Class:     be_submanifold_pentelive_Ai
 * Method:    toggleCallbacks
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_be_submanifold_pentelive_Ai_toggleCallbacks
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     be_submanifold_pentelive_Ai
 * Method:    start
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_be_submanifold_pentelive_Ai_start
  (JNIEnv *, jobject, jlong);

/*
 * Class:     be_submanifold_pentelive_Ai
 * Method:    move
 * Signature: (J[IIII)I
 */
JNIEXPORT jint JNICALL Java_be_submanifold_pentelive_Ai_move
  (JNIEnv *, jobject, jlong, jintArray, jint, jint, jint);

#ifdef __cplusplus
}
#endif
#endif
