/*
   JBlueZ - Bluetooth Java Interface for Linux Using BlueZ

   Copyright (c) 2002 The Appliance Studio Limited.
   Written by Edward Kay <ed.kay@appliancestudio.com>
   http://www.appliancestudio.com

   This program is free software; you can redistribute it and/or modify it under
   the terms of the GNU General Public License version 2 as published by the
   Free Software Foundation.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF THIRD PARTY RIGHTS.

   IN NO EVENT SHALL THE COPYRIGHT HOLDER(S) AND AUTHOR(S) BE LIABLE FOR ANY
   CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES, OR ANY DAMAGES
   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION
   OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
   CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

   ALL LIABILITY, INCLUDING LIABILITY FOR INFRINGEMENT OF ANY PATENTS,
   COPYRIGHTS, TRADEMARKS OR OTHER RIGHTS, RELATING TO USE OF THIS SOFTWARE IS
   DISCLAIMED.

   ---

   This file, bluez.c contains the native (C) code for implementing the Java
   native methods defined in BlueZ.java. These are called from Java using the
   Java Native Interface (JNI).

   The associated header file, bluez_BlueZ.h, is generated
   by Java using the 'javah' tool. Do not edit
   com_appliancestudio_jbluez_BlueZ.h - if you wish to make changes, make them
   in BlueZ.java, compile using 'javac', and then use the 'javah' tool. Further
   information regarding this process can be found in any good JNI tutorial or
   reference, or see the included Makefile.

   The purpose of this file is to expose the many functions provided by the
   BlueZ libraries to Java. For more information on what each of the functions
   do, see the BlueZ documentation (for C) or the associated Javadoc for
   BlueZ.java.

   $Id:$
*/

/* Standard includes */
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

/* JNI includes */
#include <jni.h>
#include "jbluez_BlueZ.h"

/* Bluetooth includes */
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

void throwException(JNIEnv *env, char *msg)
{
	/* Throw a BlueZException in Java, with the given message */
	jclass exception_cls;
	
	exception_cls = (*env)->FindClass(env, "jbluez/BlueZException");
	if ((*env)->ThrowNew(env, exception_cls, msg) < 0)
	{
		/* If there was a problem even throwing the exception, something */
		/* big must be wrong. Print out any info available and exit.     */
		printf("** Error throwing BlueZ exception - exiting **\n");
		printf("Message:\n%s\n", msg);
		exit(1);
	}
	return;
}

JNIEXPORT jint JNICALL Java_jbluez_hciOpenDevice
  (JNIEnv *env, jobject obj, jint hciID)
{
	/* Open the specified HCI device */
	jint dd;

	dd = hci_open_dev(hciID);
	if (dd < 0)
		throwException(env, "Java_jbluez_hciOpenDevice: HCI Device open failed");

	return dd;
}

JNIEXPORT void JNICALL Java_jbluez_hciCloseDevice
  (JNIEnv *env, jobject obj, jint dd)
{
	/* Close the specified HCI device */
	hci_close_dev(dd);
	return;
}

JNIEXPORT jint JNICALL Java_jbluez_hciCreateConnection
  (JNIEnv *env, jobject obj, jint dd, jstring bdaddr_jstr, jint ptype, jint clkoffset, jshort rswitch, jint timeOut)
{
	/* Create a connection. The handle is returned as a jint. */
	char *bdaddr_str;
	bdaddr_t bdaddr;
	uint16_t handle;

	/* Convert Java String Bluetooth address to a bdaddr_t type */
	bdaddr_str = (char*) (*env)->GetStringUTFChars(env, bdaddr_jstr, NULL);
	baswap(&bdaddr, strtoba(bdaddr_str));
	(*env)->ReleaseStringUTFChars(env, bdaddr_jstr, bdaddr_str);

	/* Create the connection */
	if (hci_create_connection(dd, &bdaddr, ptype, clkoffset, rswitch, &handle, timeOut) < 0)
	{
		throwException(env, "Java_jbluez_hciCreateConnection: Unable to create connection");
		return -1;
	}

	/* Return the handle */
	return (jint) handle;
}

JNIEXPORT void JNICALL Java_jbluez_hciDisconnect
  (JNIEnv *env, jobject obj, jint dd, jint handle, jshort reason, jint timeOut)
{
	/* Disconnect */
	if (hci_disconnect(dd, handle, 0x13, timeOut) < 0)
		throwException(env, "Java_jbluez_hciDisconnect: Unable to disconnect");

	return;
}

JNIEXPORT jobject JNICALL Java_jbluez_hciInquiry
  (JNIEnv *env, jobject obj, jint dd, jint length, jint max_num_rsp, jlong flags)
{
	/* Perform an HCI inquiry - result is returned as a */
	/* Java InquiryInfo object.                         */
	
	jclass ii_cls, iid_cls, ba_cls;
	jmethodID ii_con_id, ii_add_id, iid_con_id, ba_con_id;
	jobject info, info_dev, bdaddr;
	jvalue iid_args[8];

	int num_rsp, i;
	inquiry_info *inq_info = NULL;
	bdaddr_t bdaddr_cpy;

	char *ba_str;
	jstring jba_str;

	/* Perform the HCI inquiry */
	num_rsp = hci_inquiry(dd, length, max_num_rsp, NULL, &inq_info, flags);
	if (num_rsp < 0)
	{
		throwException(env, "Java_jbluez_hciInquiry: Inquiry failed");
		return 0;
	}

	/* Create a new instance of InquiryInfo */
	ii_cls = (*env)->FindClass(env, "jbluez/InquiryInfo");
	ii_con_id = (*env)->GetMethodID(env, ii_cls, "<init>", "(B)V");
	ii_add_id = (*env)->GetMethodID(env, ii_cls, "addDevice", "(Lcom/appliancestudio/jjbluez/InquiryInfoDevice;)V");
	info = (*env)->NewObject(env, ii_cls, ii_con_id, num_rsp);

	/* For each of the responses, create a new InquiryInfoDevice object */
	/* and add it to the InquiryInfo class.                             */
	iid_cls = (*env)->FindClass(env, "jbluez/InquiryInfoDevice");
	iid_con_id = (*env)->GetMethodID(env, iid_cls, "<init>", "(Lcom/appliancestudio/jjbluez/BTAddress;SSSSSSI)V");
	ba_cls = (*env)->FindClass(env, "jbluez/BTAddress");

	for (i=0; i<num_rsp; i++)
	{
		/* Create a new instance of BTAddress */
		ba_con_id = (*env)->GetMethodID(env, ba_cls, "<init>", "(Ljava/lang/String;)V");

		/* bdaddr - BTAddress Object */
		baswap(&bdaddr_cpy, &(inq_info+i)->bdaddr);
		ba_str = batostr(&bdaddr_cpy); // Convert from bdaddr_t to char*
		jba_str = (*env)->NewStringUTF(env, ba_str);// Convert from char* to jstring
		bdaddr = (*env)->NewObject(env, ba_cls, ba_con_id, jba_str); // Create BTAddress object

		iid_args[0].l = (jobject) bdaddr;
		iid_args[1].s = (jshort)  inq_info->pscan_rep_mode;
		iid_args[2].s = (jshort)  inq_info->pscan_period_mode;
		iid_args[3].s = (jshort)  inq_info->pscan_mode;
		iid_args[4].s = (jshort)  inq_info->dev_class[0];
		iid_args[5].s = (jshort)  inq_info->dev_class[1];
		iid_args[6].s = (jshort)  inq_info->dev_class[2];
		iid_args[7].i = (jint)    inq_info->clock_offset;

		/* Create a new instance of InquiryInfoDevice */
		info_dev = (*env)->NewObjectA(env, iid_cls, iid_con_id, iid_args);

		/* Add the new InquiryInfoDevice object to InquiryInfo */
		(*env)->CallVoidMethod(env, info, ii_add_id, info_dev);
	}
	/* Release the memory used for inq_info */
	free(inq_info);

	return info;
}

JNIEXPORT jobject JNICALL Java_jbluez_hciDevInfo
  (JNIEnv *env, jobject obj, jint hciID)
{
	/* Get the device info for a local HCI device given its device ID */
	/* Results returned as an HCIDeviceInfo object.                   */
	struct hci_dev_info di;
	struct hci_dev_stats ds;
	bdaddr_t bdaddr_cpy;
	char *bdaddr_str;
	jclass di_cls, ds_cls, df_cls, bda_cls;
	jmethodID di_con_id, ds_con_id, df_con_id, bda_con_id;
	jobject info, stat, features, bdaddr;
	jstring bdaddr_jstr;
	jvalue cons_args[14];
	int i;

	/* Get the info */
	if (hci_devinfo(hciID, &di) < 0)
	{
		throwException(env, "Java_jbluez_hciDevInfo: Failed to get device info");
		return 0;
	}

	/* Get the stats from the info structure */
	ds = di.stat;

	/*************************/
	/* HCIDeviceStats object */
	/*************************/

	/* Find the HCIDeviceStats class and constructor */
	ds_cls = (*env)->FindClass(env, "jbluez/HCIDeviceStats");
	ds_con_id = (*env)->GetMethodID(env, ds_cls, "<init>", "(JJJJJJJJJJ)V");

	/* Populate the jvalue array for passing the arguments to the HCIDeviceStats constructor */
	cons_args[0].j = (jlong) ds.err_rx;
	cons_args[1].j = (jlong) ds.err_tx;
	cons_args[2].j = (jlong) ds.cmd_tx;
	cons_args[3].j = (jlong) ds.evt_rx;
	cons_args[4].j = (jlong) ds.acl_tx;
	cons_args[5].j = (jlong) ds.acl_rx;
	cons_args[6].j = (jlong) ds.sco_tx;
	cons_args[7].j = (jlong) ds.sco_rx;
	cons_args[8].j = (jlong) ds.byte_rx;
	cons_args[9].j = (jlong) ds.byte_tx;

	/* Create a new instance of HCIDeviceStats */
	stat = (*env)->NewObjectA(env, ds_cls, ds_con_id, cons_args);

	/* ----- */

	/****************************/
	/* HCIDeviceFeatures object */
	/****************************/

	/* Find the HCIFeatures class and constructor */
	df_cls = (*env)->FindClass(env, "jbluez/HCIFeatures");
	df_con_id = (*env)->GetMethodID(env, df_cls, "<init>", "(SSSSSSSS)V");

	/* Populate the jvalue array for passing the arguments to the HCIFeatures constructor */
	for (i=0; i<8; i++)
		cons_args[i].s = (jshort) di.features[i];

	/* Create a new instance of HCIFeatures */
	features = (*env)->NewObjectA(env, df_cls, df_con_id, cons_args);

	/* ----- */

	/********************/
	/* BTAddress object */
	/********************/

	/* Create a BTAddress object for the Bluetooth device address */
	bda_cls = (*env)->FindClass(env, "jbluez/BTAddress");
	bda_con_id = (*env)->GetMethodID(env, bda_cls, "<init>", "(Ljava/lang/String;)V");
	baswap(&bdaddr_cpy, &di.bdaddr);
	bdaddr_str = batostr(&bdaddr_cpy); // Convert from bdaddr_t to char*
	bdaddr_jstr = (*env)->NewStringUTF(env, bdaddr_str);// Convert from char* to jstring
	bdaddr = (*env)->NewObject(env, bda_cls, bda_con_id, bdaddr_jstr);

	/* ----- */

	/************************/
	/* HCIDeviceInfo object */
	/************************

	/* Find the HCIDeviceInfo class and constructor */
	di_cls = (*env)->FindClass(env, "jbluez/HCIDeviceInfo");
	di_con_id = (*env)->GetMethodID(env, di_cls, "<init>", "(ILjava/lang/String;Lcom/appliancestudio/jjbluez/BTAddress;JSLcom/appliancestudio/jbluez/HCIFeatures;JJJIIIILcom/appliancestudio/jbluez/HCIDeviceStats;)V");

	/* Populate the jvalue array for passing the arguments to the HCIDeviceInfo constructor */
	cons_args[0].i  = (jint)    di.dev_id;
	cons_args[1].l  = (jobject) (*env)->NewStringUTF(env, (di.name));
	cons_args[2].l  = (jobject) bdaddr;
	cons_args[3].j  = (jlong)   di.flags;
	cons_args[4].s  = (jshort)  di.type;
	cons_args[5].l  = (jobject) features;
	cons_args[6].j  = (jlong)   di.pkt_type;
	cons_args[7].j  = (jlong)   di.link_policy;
	cons_args[8].j  = (jlong)   di.link_mode;
	cons_args[9].i  = (jint)    di.acl_mtu;
	cons_args[10].i = (jint)    di.acl_pkts;
	cons_args[11].i = (jint)    di.sco_mtu;
	cons_args[12].i = (jint)    di.sco_pkts;
	cons_args[13].l = (jobject) stat;

	/* Create a new instance of HCIDeviceInfo */
	info = (*env)->NewObjectA(env, di_cls, di_con_id, cons_args);

	/* ----- */

	/* Return the HCIDeviceInfo object */
	return info;
}

JNIEXPORT jobject JNICALL Java_jbluez_hciDevBTAddress
  (JNIEnv *env, jobject obj, jint hciID)
{
	/* Finds the Bluetooth address of the given HCI device, */
	/* returned as a Java BTAddress object.                 */
	bdaddr_t bdaddr, bdaddr_cpy;
	char *bdaddr_str;
	jclass bda_cls;
	jobject bdaddr_obj;
	jmethodID bda_con_id;
	jstring bdaddr_jstr;

	/* Get the Bluetooth address */
	if (hci_devba(hciID, &bdaddr) < 0)
	{
		throwException(env, "Java_jbluez_hciDevBTAddress: Unable to get Bluetooth address for device");
		return 0;
	}

	/* Create a BTAddress object for the Bluetooth device address */
	bda_cls = (*env)->FindClass(env, "jbluez/BTAddress");
	bda_con_id = (*env)->GetMethodID(env, bda_cls, "<init>", "(Ljava/lang/String;)V");
	baswap(&bdaddr_cpy, &bdaddr);
	bdaddr_str = batostr(&bdaddr_cpy); // Convert from bdaddr_t to char*
	bdaddr_jstr = (*env)->NewStringUTF(env, bdaddr_str);// Convert from char* to jstring
	bdaddr_obj = (*env)->NewObject(env, bda_cls, bda_con_id, bdaddr_jstr);

	/* Return the BTAddress object */
	return bdaddr_obj;
}

JNIEXPORT jint JNICALL Java_jbluez_hciDeviceID
  (JNIEnv *env, jobject obj, jstring bdaddr_jstr)
{
	/* Find the HCI device ID for a local device with the given BT address */
	/* Returns the HCI device ID as an int.                                */
	char *bdaddr_str;
	jint devID;

	/* Convert Java String Bluetooth address to a char* string */
	bdaddr_str = (char*) (*env)->GetStringUTFChars(env, bdaddr_jstr, NULL);

	/* Find the device ID */
	devID = hci_devid(bdaddr_str);
	if (devID < 0)
		throwException(env, "Java_jbluez_hciDeviceID: Unable to get device ID");

	/* Inform the Java VM the native code no longer needs access to bdaddr_str */
	(*env)->ReleaseStringUTFChars(env, bdaddr_jstr, bdaddr_str);

	return devID;
}

JNIEXPORT jstring JNICALL Java_jbluez_hciLocalName
  (JNIEnv *env, jobject obj, jint dd, jint timeOut)
{
	/* Read the name of a local Bluetooth device, returned as a Java String. */
	char name_str[248];
	jstring name_jstr;

	/* Get the name */
	if (hci_local_name(dd, sizeof(name_str), name_str, timeOut) < 0)
	{
		throwException(env, "Java_jbluez_hciLocalName: Unable to read local name");
		return 0;
	}

	/* Convert the name to a Java String */
	name_jstr = (*env)->NewStringUTF(env, name_str);

	return name_jstr;
}

JNIEXPORT jstring JNICALL Java_jbluez_hciRemoteName
  (JNIEnv *env, jobject obj, jint dd, jstring bdaddr_jstr, jint timeOut)
{
	/* Read the name of a remote Bluetooth device, returned as a Java String. */
	bdaddr_t bdaddr;
	char *bdaddr_str;
	char name_str[248];
	jstring name_jstr;

	/* Convert Java String Bluetooth address to a bdaddr_t type */
	bdaddr_str = (char*) (*env)->GetStringUTFChars(env, bdaddr_jstr, NULL);
	baswap(&bdaddr, strtoba(bdaddr_str));
	(*env)->ReleaseStringUTFChars(env, bdaddr_jstr, bdaddr_str);

	/* Get the name */
	if (hci_remote_name(dd, &bdaddr, sizeof(name_str), name_str, timeOut) < 0)
	{
		throwException(env, "Java_jbluez_hciRemoteName: Unable to read remote name");
		return 0;
	}

	/* Convert the name to a Java String */
	name_jstr = (*env)->NewStringUTF(env, name_str);

	return name_jstr;
}

JNIEXPORT jobject JNICALL Java_jbluez_hciReadRemoteFeatures
  (JNIEnv *env, jobject obj, jint dd, jint handle, jint timeOut)
{
	/* Read the features of a remote device, returned as */
	/* an HCIFeatures object.                            */
	uint8_t features[8];
	jclass features_cls;
	jmethodID features_con_id;
	jobject features_obj;
	jvalue features_args[8];
	int i;

	/* Read the remote features */
	if (hci_read_remote_features(dd, handle, features, timeOut) < 0)
	{
		throwException(env, "Java_jbluez_hciReadRemoteFeatures: Unable to read remote features");
		return 0;
	}

	/* Populate the arguments array */
	for (i=0; i<8; i++)
		features_args[i].s = (jshort) features[i];

	/* Create a new HCIFeatures object */
	features_cls = (*env)->FindClass(env, "jbluez/HCIFeatures");
	features_con_id = (*env)->GetMethodID(env, features_cls, "<init>", "(SSSSSSSS)V");
	features_obj = (*env)->NewObject(env, features_cls, features_con_id, features_args);

	/* Return the HCIFeatures object */
	return features_obj;
}

JNIEXPORT jobject JNICALL Java_jbluez_hciReadRemoteVersion
  (JNIEnv *env, jobject obj, jint dd, jint handle, jint timeOut)
{
	/* Read the version information of a remote device, returned as */
	/* an HCIVersion Java object.                                   */
	struct hci_version ver;
	jclass ver_cls;
	jmethodID ver_con_id;
	jobject ver_obj;
	jvalue ver_args[5];

	/* Read the remote version info */
	if (hci_read_remote_version(dd, (uint16_t) handle, &ver, timeOut) < 0)
	{
		throwException(env, "Java_jbluez_hciReadRemoteVersion: Unable to read remote version");
		return 0;
	}

	/* Populate the arguments array */
	ver_args[0].i = ver.manufacturer;
	ver_args[1].s = ver.hci_ver;
	ver_args[2].i = ver.hci_rev;
	ver_args[3].s = ver.lmp_ver;
	ver_args[4].i = ver.lmp_subver;

	/* Create a new HCIVersion object */
	ver_cls = (*env)->FindClass(env, "jbluez/HCIVersion");
	ver_con_id = (*env)->GetMethodID(env, ver_cls, "<init>", "(ISISI)V");
	ver_obj = (*env)->NewObjectA(env, ver_cls, ver_con_id, ver_args);

	/* Return the HCIVersion object */
	return ver_obj;
}


JNIEXPORT jobject JNICALL Java_jbluez_hciReadLocalVersion
  (JNIEnv *env, jobject obj, jint dd, jint timeOut)
{
	/* Read the version information of a local device, returned as */
	/* an HCIVersion Java object.                                  */
	struct hci_version ver;
	jclass ver_cls;
	jmethodID ver_con_id;
	jobject ver_obj;
	jvalue ver_args[5];

	/* Read the local version info */
	if (hci_read_local_version(dd, &ver, timeOut) < 0)
	{
		throwException(env, "Java_jbluez_hciReadLocalVersion: Unable to read local version");
		return 0;
	}

	/* Populate the arguments array */
	ver_args[0].i = ver.manufacturer;
	ver_args[1].s = ver.hci_ver;
	ver_args[2].i = ver.hci_rev;
	ver_args[3].s = ver.lmp_ver;
	ver_args[4].i = ver.lmp_subver;

	/* Create a new HCIVersion object */
	ver_cls = (*env)->FindClass(env, "jbluez/HCIVersion");
	ver_con_id = (*env)->GetMethodID(env, ver_cls, "<init>", "(ISISI)V");
	ver_obj = (*env)->NewObjectA(env, ver_cls, ver_con_id, ver_args);

	/* Return the HCIVersion object */
	return ver_obj;
}
