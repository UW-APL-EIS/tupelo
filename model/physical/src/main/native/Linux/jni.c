/**
 * Copyright © 2016, University of Washington
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     * Neither the name of the University of Washington nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL UNIVERSITY OF
 * WASHINGTON BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
