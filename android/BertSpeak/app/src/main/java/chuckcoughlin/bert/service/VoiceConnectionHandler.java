/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bert.service;

/**
 * The discovery fragment implements this interface in response to
 * MasterChecker activities.
 */
public interface VoiceConnectionHandler  {
    /**
     * There was an error in the connection attempt.
     * @param reason error description
     */
    public void handleNetworkError(String reason);
    /**
     * The bluetooth connection request succeeded.
     */
    public void receiveNetworkConnection();
}
