/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import java.util.EventListener;

/**
 * Implementation of a change listener for socket state
 */
public interface SocketStateChangeListener extends EventListener {
	/**
	 * @param event contains information about the source and the new state.
	 */
	public void stateChanged(SocketStateChangeEvent event);
}