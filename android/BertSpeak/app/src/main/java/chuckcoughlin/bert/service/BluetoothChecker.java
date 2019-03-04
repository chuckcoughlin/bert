/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bert.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import chuckcoughlin.bert.common.BertConstants;
import chuckcoughlin.bert.db.BertDbManager;

/**
 * Attempt to create a network connection via Bluetooth.
 * NOTE: Bluetooth is not supported on the emulator. The adapter will be null.
 */
public class BluetoothChecker {
    private final static String CLSS = "BluetoothChecker";
    private CheckerThread checkerThread = null;
    private final String device;
    private VoiceServiceHandler handler;
    private boolean threadRunning;

    public BluetoothChecker(VoiceServiceHandler handler) {
        this.threadRunning = false;
        this.handler = handler;
        this.device  = BertDbManager.getInstance().getSetting(BertConstants.BERT_PAIRED_DEVICE);
    }

    // An empty string returned implies success, else an error message.
    // For now we always return an error
    public String bluetoothValid(BluetoothAdapter adapter) {
        String errorMsg = "";
        if( adapter==null ) {
            errorMsg = "There is no bluetooth network";
        }
        else if( device==null || device.isEmpty() ) {
            errorMsg = "No bluetooth device has been specified";
        }
        else {
            if( !adapter.isEnabled() ) adapter.enable();
            if( !adapter.isEnabled()) {
                errorMsg = "The bluetooth network is not enabled";
            }
        }
        return errorMsg;
    }

    public void beginChecking(BluetoothManager bmgr) {
        if( this.threadRunning ) {
            Log.i(CLSS, "check already in progress ...");
            return;
        }
        String errMsg = bluetoothValid(bmgr.getAdapter());
        if( errMsg.isEmpty() ) {
            checkerThread = new CheckerThread(bmgr);
            checkerThread.start();
        }
        else {
            handler.handleBluetoothError(errMsg);
        }
    }

    public void stopChecking() {
        if (checkerThread != null && checkerThread.isAlive()) {
            checkerThread.interrupt();
        }
    }

    private class CheckerThread extends Thread {
        private BluetoothAdapter adapter;
        private Set<BluetoothDevice> pairedDevices;

        public CheckerThread(BluetoothManager bmgr) {
            this.adapter = bmgr.getAdapter();
            this.pairedDevices = new HashSet<>();
            setDaemon(true);
            // don't require callers to explicitly kill all the old checker threads.
            setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    String msg = String.format("There was an uncaught exception checking bluetooth: %s",ex.getLocalizedMessage());
                    Log.e(CLSS,msg ,ex);
                    handler.handleBluetoothError(msg);
                    threadRunning = false;
                }
            });
        }


        @Override
        public void run() {
            threadRunning = true;
            boolean success = false;
            String errorMsg = "";
            try {
                adapter.startDiscovery();
                int cycle = 0;
                while (cycle < 10 && !success) {
                    pairedDevices = adapter.getBondedDevices();
                    for (BluetoothDevice bluetoothDevice : pairedDevices) {
                        if( cycle==9 ) Log.i(CLSS, String.format("BluetoothChecker: Found %s %s %s", bluetoothDevice.getName(), bluetoothDevice.getType(), bluetoothDevice.getAddress()));
                        if( bluetoothDevice.getName().equalsIgnoreCase(device) ) {
                            success = true;
                            break;
                        }
                    }
                    Thread.sleep(1000);
                    cycle++;
                }
                adapter.cancelDiscovery();
            }
            catch (Throwable ex) {
                errorMsg = String.format("%s while searching Bluetooth devices for %s",ex.getLocalizedMessage(),device);
                Log.e(CLSS, errorMsg,ex);
            }

            if (success) {
                handler.receiveBluetoothConnection();
            }
            else {
                handler.handleBluetoothError(errorMsg);
            }
            threadRunning = false;
        }
    }
}