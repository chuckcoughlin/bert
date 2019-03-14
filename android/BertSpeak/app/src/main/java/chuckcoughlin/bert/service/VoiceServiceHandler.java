/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bert.service;

import android.bluetooth.BluetoothDevice;

/**
 * The only implementation of this interface is the VoiceService itself.
 */
public interface VoiceServiceHandler {
    /**
     * There was an error in the Bluetooth connection attempt.
     * @param reason error description
     */
    public void handleBluetoothError(String reason);
    /**
     * The bluetooth connection request succeeded.
     */
    public void receiveBluetoothConnection();
    /**
     * There was an error in the connection attempt.
     * @param reason error description
     */
    public void handleSocketError(String reason);
    /**
     * The socket connection request succeeded.
     */
    public void receiveSocketConnection();
    /**
     * Yhre speech recognizer reported an error.
     * @param reason error description
     */
    public void handleVoiceError(String reason);
    /**
     * The speech recognizer recorded a result.
     */
    public void receiveText(String text);

    /**
     * Save the structure of the discovered device.
     * @param device the Bluetooth device representing the robot.
     */
    public void setBluetoothDevice(BluetoothDevice device) ;
}
