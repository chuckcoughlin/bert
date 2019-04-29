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

#define NOT_DISCOVERABLE bluecove_bluetooth_BluetoothStackBlueZConsts_NOT_DISCOVERABLE
#define GIAC             bluecove_bluetooth_BluetoothStackBlueZConsts_GIAC
#define LIAC             bluecove_bluetooth_BluetoothStackBlueZConsts_LIAC

#define INQUIRY_COMPLETED  bluecove_bluetooth_BluetoothStackBlueZConsts_INQUIRY_COMPLETED
#define INQUIRY_TERMINATED bluecove_bluetooth_BluetoothStackBlueZConsts_INQUIRY_TERMINATED
#define INQUIRY_ERROR      bluecove_bluetooth_BluetoothStackBlueZConsts_INQUIRY_ERROR

#define SERVICE_SEARCH_COMPLETED            bluecove_bluetooth_BluetoothStackBlueZConsts_SERVICE_SEARCH_COMPLETED
#define SERVICE_SEARCH_TERMINATED           bluecove_bluetooth_BluetoothStackBlueZConsts_SERVICE_SEARCH_TERMINATED
#define SERVICE_SEARCH_ERROR                bluecove_bluetooth_BluetoothStackBlueZConsts_SERVICE_SEARCH_ERROR
#define SERVICE_SEARCH_NO_RECORDS           bluecove_bluetooth_BluetoothStackBlueZConsts_SERVICE_SEARCH_NO_RECORDS
#define SERVICE_SEARCH_DEVICE_NOT_REACHABLE bluecove_bluetooth_BluetoothStackBlueZConsts_SERVICE_SEARCH_DEVICE_NOT_REACHABLE

/* Taken from ServiceRecord.java */
#define NOAUTHENTICATE_NOENCRYPT 0
#define AUTHENTICATE_NOENCRYPT   1
#define AUTHENTICATE_ENCRYPT     2


#define DATA_ELEMENT_TYPE_NULL     bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_NULL
#define DATA_ELEMENT_TYPE_U_INT_1  bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_U_INT_1
#define DATA_ELEMENT_TYPE_U_INT_2  bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_U_INT_2
#define DATA_ELEMENT_TYPE_U_INT_4  bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_U_INT_4
#define DATA_ELEMENT_TYPE_U_INT_8  bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_U_INT_8
#define DATA_ELEMENT_TYPE_U_INT_16 bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_U_INT_16
#define DATA_ELEMENT_TYPE_INT_1    bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_INT_1
#define DATA_ELEMENT_TYPE_INT_2    bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_INT_2
#define DATA_ELEMENT_TYPE_INT_4    bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_INT_4
#define DATA_ELEMENT_TYPE_INT_8    bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_INT_8
#define DATA_ELEMENT_TYPE_INT_16   bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_INT_16
#define DATA_ELEMENT_TYPE_URL      bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_URL
#define DATA_ELEMENT_TYPE_UUID     bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_UUID
#define DATA_ELEMENT_TYPE_BOOL     bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_BOOL
#define DATA_ELEMENT_TYPE_STRING   bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_STRING
#define DATA_ELEMENT_TYPE_DATSEQ   bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_DATSEQ
#define DATA_ELEMENT_TYPE_DATALT   bluecove_bluetooth_BluetoothStackBlueZConsts_DataElement_DATALT

#define BT_CONNECTION_ERROR_UNKNOWN_PSM         bluecove_bluetooth_BluetoothStackBlueZConsts_CONNECTION_ERROR_UNKNOWN_PSM
#define BT_CONNECTION_ERROR_SECURITY_BLOCK      bluecove_bluetooth_BluetoothStackBlueZConsts_CONNECTION_ERROR_SECURITY_BLOCK
#define BT_CONNECTION_ERROR_NO_RESOURCES        bluecove_bluetooth_BluetoothStackBlueZConsts_CONNECTION_ERROR_NO_RESOURCES
#define BT_CONNECTION_ERROR_FAILED_NOINFO       bluecove_bluetooth_BluetoothStackBlueZConsts_CONNECTION_ERROR_FAILED_NOINFO
#define BT_CONNECTION_ERROR_TIMEOUT             bluecove_bluetooth_BluetoothStackBlueZConsts_CONNECTION_ERROR_TIMEOUT
#define BT_CONNECTION_ERROR_UNACCEPTABLE_PARAMS bluecove_bluetooth_BluetoothStackBlueZConsts_CONNECTION_ERROR_UNACCEPTABLE_PARAMS

#define BLUEZ_VERSION_MAJOR_3 3
#define BLUEZ_VERSION_MAJOR_4 4

int getBlueZVersionMajor(JNIEnv* env);

sdp_record_t* bluecove_sdp_extract_pdu(JNIEnv* env, const uint8_t *pdata, int bufsize, int *scanned);

#endif  /* _BLUECOVEBLUEZ_H */

