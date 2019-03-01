/**
 *   (c) 2019  Charles Coughlin. All rights reserved.
 *   (MIT License)
 */
package chuckcoughlin.bert.service;

/**
 * This enumeration class represents the permissible states of the voice service.
 */
public enum ConnectionState {
	NONE,
	NETWORK,
	SOCKET,
	TALKING
	;

	/**
	 * @return  a comma-separated list of all state values in a single String.
	 */
	public static String names() {
		StringBuffer names = new StringBuffer();
		for (ConnectionState state : ConnectionState.values())
		{
			names.append(state.name()+", ");
		}
		return names.substring(0, names.length()-2);
	}
}
