/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bert.service;

/**
 * The only implementation of this interface is the VoiceService itself.
 */
public interface VoiceServiceHandler {
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
}
