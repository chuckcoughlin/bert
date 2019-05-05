/*
 *
 *  BlueZ - Bluetooth protocol stack for Linux
 *
 *  Copyright (C) 2011-2012 David Herrmann <dh.herrmann@googlemail.com>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <unistd.h>
 
 
#include <stdbool.h>

#include <bluetooth/bluetooth.h>
#include <glib.h>

#include "lib/uuid.h"
#include "src/plugin.h"
#include "src/adapter.h"
#include "src/shared/util.h"
#include "src/log.h"
#include "attrib/gattrib.h"
#include "attrib/gatt-service.h"
#include "attrib/att.h"
#include "attrib/gatt.h"
#include "attrib/att-database.h"
#include "src/attrib-server.h"


#define MAX_STR_LEN       (256)

#define SIMPLE_SVC_UUID      0xfff0
#define SIMPLE_READ1_CHAR_UUID    0xfff1
#define SIMPLE_READ2_CHAR_UUID    0xfff2
#define SIMPLE_WRITE_CHAR_UUID    0xfff3  
#define SIMPLE_NOTIFY_CHAR_UUID    0xfff4


static char read1Data[MAX_STR_LEN];
static char read2Data[MAX_STR_LEN];

static int notifyData;

static uint8_t SimpleCharacteristic1Read(struct attribute *a,
      struct btd_device *device, gpointer user_data)
{
 struct btd_adapter *adapter;
 
 printf("__FILE__ = %s, __FUNCTION__ = %s, __LINE__ =%d\n", 
  __FILE__, __FUNCTION__, __LINE__);
 
 
 adapter = user_data;
 
 attrib_db_update(adapter, a->handle, NULL, 
  (uint8_t*)&read1Data[0], strlen(&read1Data[0]), NULL);
 
 return 0;
}/*Characteristic1Read*/      

     

static uint8_t SimpleCharacteristic2Read(struct attribute *a,
      struct btd_device *device, gpointer user_data)
{
 struct btd_adapter *adapter;
 
 printf("__FILE__ = %s, __FUNCTION__ = %s, __LINE__ =%d\n", 
  __FILE__, __FUNCTION__, __LINE__);
 
  
 adapter = user_data;
 
 attrib_db_update(adapter, a->handle, NULL, 
  (uint8_t*)&read2Data[0], strlen(&read2Data[0]), NULL);
  
 return 0;
}/*Characteristic2Read*/ 


static uint8_t SimpleCharacteristicWrite(struct attribute *a,
      struct btd_device *device, gpointer user_data)
{

 unsigned char data[MAX_STR_LEN];
 int i;
 
 printf("__FILE__ = %s, __FUNCTION__ = %s, __LINE__ =%d\n", 
  __FILE__, __FUNCTION__, __LINE__);
 
 memset(&data[0], 0, MAX_STR_LEN);
 
 memcpy(&data[0], a->data, a->len);
  
 printf("written data : %s \n", &data[0]); 

 for(i = 0; i< a->len;i++)
  printf("%#1x ", (unsigned char)(data[i]));
 printf("\n"); 
 
 return 0;
}/*CharacteristicWrite*/


static uint8_t SimpleCharacteristicNotify(struct attribute *a,
      struct btd_device *device, gpointer user_data)
{
 struct btd_adapter *adapter;
 
 adapter = user_data;
 
 
 printf("__FILE__ = %s, __FUNCTION__ = %s, __LINE__ =%d\n", 
  __FILE__, __FUNCTION__, __LINE__);
 
 do
 { 
  attrib_db_update(adapter, a->handle, NULL, 
   (uint8_t*)&notifyData, sizeof(notifyData), NULL);  
   
  //usleep(1*1000*1000);
  notifyData++; 
 }while(0); 
 
 return 0;
}/*CharacteristicNotify*/


static void RegisterSimpleService(struct btd_adapter *adapter)
{
 bt_uuid_t uuid;
 printf("__FILE__ = %s, __FUNCTION__ = %s, __LINE__ =%d\n", 
  __FILE__, __FUNCTION__, __LINE__);
 
 bt_uuid16_create(&uuid, SIMPLE_SVC_UUID);
 
 gatt_service_add(adapter, GATT_PRIM_SVC_UUID, &uuid,
 
 /* characteristic register*/
 
   /*read 1*/
   GATT_OPT_CHR_UUID16, SIMPLE_READ1_CHAR_UUID,
   GATT_OPT_CHR_PROPS, GATT_CHR_PROP_READ ,
   GATT_OPT_CHR_VALUE_CB, ATTRIB_READ,
     SimpleCharacteristic1Read, adapter,
     
   /*read 2*/
   GATT_OPT_CHR_UUID16, SIMPLE_READ1_CHAR_UUID,
   GATT_OPT_CHR_PROPS, GATT_CHR_PROP_READ ,
   GATT_OPT_CHR_VALUE_CB, ATTRIB_READ,
     SimpleCharacteristic2Read, adapter,

   /*write*/
   GATT_OPT_CHR_UUID16, SIMPLE_WRITE_CHAR_UUID,
   GATT_OPT_CHR_PROPS, GATT_CHR_PROP_WRITE_WITHOUT_RESP,
   GATT_OPT_CHR_VALUE_CB, ATTRIB_WRITE,
     SimpleCharacteristicWrite, adapter,

   /*NOTIFY*/
   GATT_OPT_CHR_UUID16, SIMPLE_NOTIFY_CHAR_UUID,
   GATT_OPT_CHR_PROPS, GATT_CHR_PROP_READ|GATT_CHR_PROP_NOTIFY,
   GATT_OPT_CHR_VALUE_CB, ATTRIB_READ,
     SimpleCharacteristicNotify, adapter,     
   /*end*/    
 GATT_OPT_INVALID);
 
 return ;      
}/*RegisterSimpleService*/

#define DEVICEINFO_SVC_UUID     0x180a 

char versionStr[MAX_STR_LEN] = "0.0.1";
char manufacturerStr[MAX_STR_LEN] = "Gaiger";   


static uint8_t SoftwareRevisionStringRead(struct attribute *a,
      struct btd_device *device, gpointer user_data)
{
 struct btd_adapter *adapter;
 
 
 printf("__FILE__ = %s, __FUNCTION__ = %s, __LINE__ =%d\n", 
  __FILE__, __FUNCTION__, __LINE__);
   
 adapter = user_data;
 
 attrib_db_update(adapter, a->handle, NULL, 
  (uint8_t*)&versionStr[0], strlen(&versionStr[0]), NULL);
  
 return 0;
}/*SoftwareRevisionStringRead*/ 


static uint8_t ManufacturerStringRead(struct attribute *a,
      struct btd_device *device, gpointer user_data)
{
 struct btd_adapter *adapter;
 
 printf("__FILE__ = %s, __FUNCTION__ = %s, __LINE__ =%d\n", 
  __FILE__, __FUNCTION__, __LINE__);
   
 adapter = user_data;
 
 attrib_db_update(adapter, a->handle, NULL, 
  (uint8_t*)&manufacturerStr[0], strlen(&manufacturerStr[0]), NULL);
  
 return 0;
}/*ManufacturerStringRead*/ 


static void RegisterDeviceInfo(struct btd_adapter *adapter)
{
 bt_uuid_t uuid;
 printf("__FILE__ = %s, __FUNCTION__ = %s, __LINE__ =%d\n", 
  __FILE__, __FUNCTION__, __LINE__);
 
 bt_uuid16_create(&uuid, DEVICEINFO_SVC_UUID);
 
 gatt_service_add(adapter, GATT_PRIM_SVC_UUID, &uuid,
 
 /* characteristic register*/
     
   /*GATT_CHARAC_SOFTWARE_REVISION_STRING*/
   GATT_OPT_CHR_UUID16, GATT_CHARAC_SOFTWARE_REVISION_STRING,
   GATT_OPT_CHR_PROPS, GATT_CHR_PROP_READ ,
   GATT_OPT_CHR_VALUE_CB, ATTRIB_READ,
     SoftwareRevisionStringRead, adapter,
   
   /*GATT_CHARAC_MANUFACTURER_NAME_STRING*/
   GATT_OPT_CHR_UUID16, GATT_CHARAC_MANUFACTURER_NAME_STRING,
   GATT_OPT_CHR_PROPS, GATT_CHR_PROP_READ ,
   GATT_OPT_CHR_VALUE_CB, ATTRIB_READ,
     ManufacturerStringRead, adapter, 
   /*end*/    
 GATT_OPT_INVALID);
 
 return ;      
}/*RegisterSimpleService*/

static void update_name(struct btd_adapter *adapter, gpointer user_data)
{
 adapter_set_name(adapter, (char*)user_data);
}/*update_name*/



static int wii_probe(struct btd_adapter *adapter)
{
  
 update_name(adapter, "SimplePeripherial");
 RegisterDeviceInfo(adapter);
 RegisterSimpleService(adapter);
 
 return 0;
}/*wii_probe*/


static void wii_remove(struct btd_adapter *adapter)
{

}/*wii_remove*/

/*function pointers*/
static struct btd_adapter_driver wii_driver = {
 .name = "wiimote",
 .probe = wii_probe,
 .remove = wii_remove,
};


static int wii_init(void)
{
 printf("__FUNCTION__ = %s\n", __FUNCTION__);
 
 
 memset(&read1Data[0], 0, MAX_STR_LEN);
 memset(&read2Data[0], 0, MAX_STR_LEN);
 notifyData = 0;
 
 snprintf(&read1Data[0], MAX_STR_LEN, "it is read 1");
 snprintf(&read2Data[0], MAX_STR_LEN, "it is read 2");
 
 return btd_register_adapter_driver(&wii_driver);
}

static void wii_exit(void)
{
 printf("__FUNCTION__ = %s\n", __FUNCTION__);
 btd_unregister_adapter_driver(&wii_driver);
}

BLUETOOTH_PLUGIN_DEFINE(wiimote, VERSION,
  BLUETOOTH_PLUGIN_PRIORITY_LOW, wii_init, wii_exit)
