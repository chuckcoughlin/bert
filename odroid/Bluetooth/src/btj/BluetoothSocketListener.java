/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package btj;

/**
 * Receive asynchronous notification of an incoming String.
 */
public interface BluetoothSocketListener {
	public String receiveString(String text);

}
