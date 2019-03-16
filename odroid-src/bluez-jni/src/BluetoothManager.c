/*
 * Mirrors the java class of the same name.
 * It is imperative that it contain all of the interfaces.
 */
include <jni.h>
include "bluezlib_BluetoothManager.h"

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    getNativeAPIVersion
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_bluezlib_BluetoothManager_getNativeAPIVersion
  (JNIEnv *, jclass);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    getBluetoothType
 * Signature: ()Lbluezlib/BluetoothType;
 */
JNIEXPORT jobject JNICALL Java_bluezlib_BluetoothManager_getBluetoothType
  (JNIEnv *, jobject);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    find
 * Signature: (ILjava/lang/String;Ljava/lang/String;Lbluezlib/BluetoothObject;J)Lbluezlib/BluetoothObject;
 */
JNIEXPORT jobject JNICALL Java_bluezlib_BluetoothManager_find
  (JNIEnv *, jobject, jint, jstring, jstring, jobject, jlong);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    getObject
 * Signature: (ILjava/lang/String;Ljava/lang/String;Lbluezlib/BluetoothObject;)Lbluezlib/BluetoothObject;
 */
JNIEXPORT jobject JNICALL Java_bluezlib_BluetoothManager_getObject
  (JNIEnv *, jobject, jint, jstring, jstring, jobject);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    getObjects
 * Signature: (ILjava/lang/String;Ljava/lang/String;Lbluezlib/BluetoothObject;)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_bluezlib_BluetoothManager_getObjects
  (JNIEnv *, jobject, jint, jstring, jstring, jobject);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    getAdapters
 * Signature: ()Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_bluezlib_BluetoothManager_getAdapters
  (JNIEnv *, jobject);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    getDevices
 * Signature: ()Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_bluezlib_BluetoothManager_getDevices
  (JNIEnv *, jobject);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    getServices
 * Signature: ()Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_bluezlib_BluetoothManager_getServices
  (JNIEnv *, jobject);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    setDefaultAdapter
 * Signature: (Lbluezlib/BluetoothAdapter;)Z
 */
JNIEXPORT jboolean JNICALL Java_bluezlib_BluetoothManager_setDefaultAdapter
  (JNIEnv *, jobject, jobject);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    startDiscovery
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_bluezlib_BluetoothManager_startDiscovery
  (JNIEnv *, jobject);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    stopDiscovery
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_bluezlib_BluetoothManager_stopDiscovery
  (JNIEnv *, jobject);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    getDiscovering
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_bluezlib_BluetoothManager_getDiscovering
  (JNIEnv *, jobject);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_bluezlib_BluetoothManager_init
  (JNIEnv *, jobject);

/*
 * Class:     bluezlib_BluetoothManager
 * Method:    delete
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_bluezlib_BluetoothManager_delete
  (JNIEnv *, jobject);

