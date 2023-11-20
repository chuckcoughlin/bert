/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.bluetooth.BluetoothDevice

/**
 * The only implementation of this interface is the DispatchService itself.
 */
interface BluetoothHandler {
    /**
     * There was an error in the Bluetooth connection attempt.
     * @param reason error description
     */
    fun handleBluetoothError(reason: String)

    /**
     * The bluetooth connection request succeeded.
     */
    fun receiveBluetoothConnection()

    /**
     * There was an error in the connection attempt.
     * @param reason error description
     */
    fun handleSocketError(reason: String)

    /**
     * The socket connection request succeeded.
     */
    fun receiveSocketConnection()

    /**
     * Yhre speech recognizer reported an error.
     * @param reason error description
     */
    fun handleVoiceError(reason: String)

    /**
     * The bluetooth connection received a message
     */
    fun receiveText(text: String)

    /**
     * Modify status screen
     */
    fun reportConnectionState(fac: ControllerType, state: ControllerState)

    /**
     * The speech recognizer recorded a result.
     */
    fun receiveSpokenText(text: String)

    /**
     * Save the structure of the discovered device.
     * @param device the Bluetooth device representing the robot.
     */
    fun setBluetoothDevice(device: BluetoothDevice?)
}
