/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bert.service;

import android.bluetooth.BluetoothDevice;

/**
 * The only implementation of this interface is the DispatchService itself.
 */
public interface BluetoothHandler {
    /**
     * There was an error in the Bluetooth connection attempt.
     * @param reason error description
     */
    void handleBluetoothError(String reason);
    /**
     * The bluetooth connection request succeeded.
     */
    void receiveBluetoothConnection();
    /**
     * There was an error in the connection attempt.
     * @param reason error description
     */
    void handleSocketError(String reason);
    /**
     * The socket connection request succeeded.
     */
    void receiveSocketConnection();
    /**
     * Yhre speech recognizer reported an error.
     * @param reason error description
     */
    void handleVoiceError(String reason);
    /**
     * The bluetooth connection received a message
     */
    void receiveText(String text);
    /**
     * Modify status screen
     */
    void reportConnectionState(TieredFacility fac, FacilityState state);
    /**
     * The speech recognizer recorded a result.
     */
    void receiveSpokenText(String text);
    /**
     * Save the structure of the discovered device.
     * @param device the Bluetooth device representing the robot.
     */
    void setBluetoothDevice(BluetoothDevice device) ;
}
