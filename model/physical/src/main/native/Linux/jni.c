#include "edu_uw_apl_tupelo_model_PhysicalDisk.h"

#include "impl.h"

/**
  In all routines here, drop into local C land for the actual logic.
  Ensures standalone testability of the C side.  Only do JNI calls
  here. See also ./impl.[ch] for the actual work.
*/

/*
 * Class:     edu_uw_apl_tupelo_model_PhysicalDisk
 * Method:    size
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_edu_uw_apl_tupelo_model_PhysicalDisk_size
(JNIEnv *env, jobject thiz, jstring path ) {

  char* pathC = (char*)(*env)->GetStringUTFChars( env, path, NULL );
  if( pathC == NULL ) {
	/* out of memory */
	return -1; 
  }

  int64_t result = diskSize( pathC );

  (*env)->ReleaseStringUTFChars(env, path, pathC );
  return (jlong)result;
}

/*
 * Class:     edu_uw_apl_tupelo_model_PhysicalDisk
 * Method:    vendorID
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_edu_uw_apl_tupelo_model_PhysicalDisk_vendorID
(JNIEnv *env, jobject thiz, jstring path ) {

  char* pathC = (char*)(*env)->GetStringUTFChars( env, path, NULL );
  if( pathC == NULL ) {
	/* out of memory */
	return NULL; 
  }

  char vendorID[32] = { 0 };
  char productID[32] = { 0 };
  char serialNumber[32] = { 0 };
  int sc = scsiInquiry( pathC, vendorID, productID, serialNumber );
  (*env)->ReleaseStringUTFChars( env, path, pathC );
  if( sc )
	return NULL;
  return (*env)->NewStringUTF( env, vendorID );
}

/*
 * Class:     edu_uw_apl_tupelo_model_PhysicalDisk
 * Method:    productID
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_edu_uw_apl_tupelo_model_PhysicalDisk_productID
(JNIEnv *env, jobject thiz, jstring path ) {

  char* pathC = (char*)(*env)->GetStringUTFChars( env, path, NULL );
  if( pathC == NULL ) {
	/* out of memory */
	return NULL; 
  }

  char vendorID[32] = { 0 };
  char productID[32] = { 0 };
  char serialNumber[32] = { 0 };
  int sc = scsiInquiry( pathC, vendorID, productID, serialNumber );
  (*env)->ReleaseStringUTFChars( env, path, pathC );
  if( sc )
	return NULL;
  return (*env)->NewStringUTF( env, productID );
}

/*
 * Class:     edu_uw_apl_tupelo_model_PhysicalDisk
 * Method:    serialNumber
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_edu_uw_apl_tupelo_model_PhysicalDisk_serialNumber
(JNIEnv *env, jobject thiz, jstring path ) {

  char* pathC = (char*)(*env)->GetStringUTFChars( env, path, NULL );
  if( pathC == NULL ) {
	/* out of memory */
	return NULL; 
  }

  char vendorID[32] = { 0 };
  char productID[32] = { 0 };
  char serialNumber[32] = { 0 };
  int sc = scsiInquiry( pathC, vendorID, productID, serialNumber );
  (*env)->ReleaseStringUTFChars( env, path, pathC );
  if( sc )
	return NULL;
  return (*env)->NewStringUTF( env, serialNumber );
}

// eof
