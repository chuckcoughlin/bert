/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mina Shokry
 *  Copyright (C) 2008 Vlad Skarzhevskyy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Author: mina
 * Created on December 24, 2007, 4:17 PM
 *
 * @version $Id: BlueCoveBlueZ.h 2443 2008-11-10 23:46:15Z minashokry $
 */

#ifndef _BLUECOVEBLUEZ_H
#define _BLUECOVEBLUEZ_H

#include <jni.h>
#include <unistd.h>
#include <errno.h>
#include <malloc.h>

#include <bluetooth/bluetooth.h>
#include <bluetooth/sdp.h>
#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>

#include "bluecove_bluetooth_BluetoothStackBlueZ.h"
#include "common.h"

#ifndef SOCKET_ERROR
#define SOCKET_ERROR   (-1)
#endif

#define LOCALDEVICE_ACCESS_TIMEOUT 5000
#define READ_REMOTE_NAME_TIMEOUT 5000
#define DEVICE_NAME_MAX_SIZE 248

int deviceClassBytesToInt(uint8_t* deviceClass);

jlong deviceAddrToLong(bdaddr_t* address);
void longToDeviceAddr(jlong addr, bdaddr_t* address);

void reverseArray(jbyte* array,int length);

void convertUUIDByteArrayToUUID(JNIEnv *env, jbyteArray byteArray, uuid_t* uuid);
void convertUUIDBytesToUUID(jbyte *bytes, uuid_t* uuid);

void debugServiceRecord(JNIEnv *env, sdp_record_t* sdpRecord);

jlong ptr2jlong(void * ptr);
void* jlong2ptr(jlong l);

/* The following are copied from DiscoveryAgent.java */
#define NOT_DISCOVERABLE 0
#define GIAC             0x9E8B33
#define LIAC             0x9E8B00

/* The following are copied from DiscoveryListener.java */
#define INQUIRY_COMPLETED  0x00
#define INQUIRY_TERMINATED 0x05
#define INQUIRY_ERROR      0x07

#define SERVICE_SEARCH_COMPLETED  0x01
#define SERVICE_SEARCH_TERMINATED 0x02
#define SERVICE_SEARCH_ERROR      0x03
#define SERVICE_SEARCH_NO_RECORDS 0x04
#define SERVICE_SEARCH_DEVICE_NOT_REACHABLE 0x06

/* Taken from ServiceRecord.java */
#define NOAUTHENTICATE_NOENCRYPT 0
#define AUTHENTICATE_NOENCRYPT   1
#define AUTHENTICATE_ENCRYPT     2


#define RETRIEVEDEVICES_OPTION_CACHED 0x00
#define RETRIEVEDEVICES_OPTION_PREKNOWN 0x01

#define DATA_ELEMENT_TYPE_NULL 0x0000
#define DATA_ELEMENT_TYPE_U_INT_1 0x0008
#define DATA_ELEMENT_TYPE_U_INT_2 0x0009
#define DATA_ELEMENT_TYPE_U_INT_4 0x000A
#define DATA_ELEMENT_TYPE_U_INT_8 0x000B
#define DATA_ELEMENT_TYPE_U_INT_16 0x000C
#define DATA_ELEMENT_TYPE_INT_1 0x0010
#define DATA_ELEMENT_TYPE_INT_2 0x0011
#define DATA_ELEMENT_TYPE_INT_4 0x0012
#define DATA_ELEMENT_TYPE_INT_8 0x0013
#define DATA_ELEMENT_TYPE_INT_16 0x0014
#define DATA_ELEMENT_TYPE_URL 0x0040
#define DATA_ELEMENT_TYPE_UUID 0x0018
#define DATA_ELEMENT_TYPE_BOOL 0x0028
#define DATA_ELEMENT_TYPE_STRING 0x0020
#define DATA_ELEMENT_TYPE_DATSEQ 0x0030
#define DATA_ELEMENT_TYPE_DATALT 0x0038

#define BT_CONNECTION_ERROR_UNKNOWN_PSM  1
#define BT_CONNECTION_ERROR_SECURITY_BLOCK 2
#define BT_CONNECTION_ERROR_NO_RESOURCES 3
#define BT_CONNECTION_ERROR_FAILED_NOINFO 4
#define BT_CONNECTION_ERROR_TIMEOUT 5
#define BT_CONNECTION_ERROR_UNACCEPTABLE_PARAMS 6

#define BLUEZ_VERSION_MAJOR_3 3
#define BLUEZ_VERSION_MAJOR_4 4

int getBlueZVersionMajor(JNIEnv* env);

sdp_record_t* bluecove_sdp_extract_pdu(JNIEnv* env, const uint8_t *pdata, int bufsize, int *scanned);

#endif  /* _BLUECOVEBLUEZ_H */

