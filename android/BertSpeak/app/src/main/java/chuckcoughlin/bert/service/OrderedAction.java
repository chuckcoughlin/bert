/**
 *   (c) 2019  Charles Coughlin. All rights reserved.
 *   (MIT License)
 */
package chuckcoughlin.bert.service;

/**
 * These are the action categories that are part of the VoiceService. The
 * actions must succeed in order.
 */
public enum OrderedAction {
	BLUETOOTH,
    SOCKET,
    VOICE
	;

	/**
	 * @return  a comma-separated list of all state values in a single String.
	 */
	public static String names() {
		StringBuffer names = new StringBuffer();
		for (OrderedAction state : OrderedAction.values())
		{
			names.append(state.name()+", ");
		}
		return names.substring(0, names.length()-2);
	}
}
