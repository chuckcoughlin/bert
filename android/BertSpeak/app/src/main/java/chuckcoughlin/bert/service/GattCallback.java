/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *  Listen for Gatt action
 */
package chuckcoughlin.bert.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

public class GattCallback extends BluetoothGattCallback {
    private static final String CLSS = "GattCallback";

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.i(CLSS,String.format("onCharacteristicChanged"));
    }
    @Override
    public void onCharacteristicRead (BluetoothGatt gatt,BluetoothGattCharacteristic characteristic,int status) {
        Log.i(CLSS,String.format("onCharacteristicRead"));
    }
    @Override
    public void onCharacteristicWrite (BluetoothGatt gatt,BluetoothGattCharacteristic characteristic,int status) {
        Log.i(CLSS,String.format("onCharacteristicWrite"));
    }
    @Override
    public void onConnectionStateChange (BluetoothGatt gatt,int status,int newState) {
        String state = "UNKNOWN";
        if( newState==BluetoothProfile.STATE_DISCONNECTED ) state = "DISCONNECTED";
        else if(newState==BluetoothProfile.STATE_CONNECTED) state = "CONNECTED";
        Log.i(CLSS,String.format("onConnectionStateChange> now %s",state));
    }
    @Override
    public void onDescriptorRead (BluetoothGatt gatt,BluetoothGattDescriptor descriptor,int status) {
        Log.i(CLSS,String.format("onDescriptorRead"));
    }
    @Override
    public void onMtuChanged (BluetoothGatt gatt,int mtu,int status) {
        Log.i(CLSS,String.format("onMtuChanged"));
    }
    @Override
    public void onPhyRead (BluetoothGatt gatt, int txPhy,int rxPhy,int status) {
        Log.i(CLSS,String.format("onPhyRead"));
    }
    @Override
    public void onPhyUpdate (BluetoothGatt gatt, int txPhy, int rxPhy,int status) {
        Log.i(CLSS,String.format("onPhyUpdate"));
    }
    @Override
    public void onReadRemoteRssi (BluetoothGatt gatt,int rssi,int status) {
        Log.i(CLSS,String.format("onReadRemoteRssi"));
    }
    @Override
    public void onReliableWriteCompleted (BluetoothGatt gatt,int status){
        Log.i(CLSS,String.format("onReliableWriteCompleted"));
    }
}
