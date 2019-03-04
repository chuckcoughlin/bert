/**
 *   (c) 2019  Charles Coughlin. All rights reserved.
 *   (MIT License)
 */
package chuckcoughlin.bert.speech;

/**
 * The message type affects the rendering and disposition of the message.
 */
public enum MessageType {
	REQUEST,
    RESPONSE,
    ERROR
	;

	/**
	 * @return  a comma-separated list of all facilities in a single String.
	 */
	public static String names() {
		StringBuffer names = new StringBuffer();
		for (MessageType state : MessageType.values())
		{
			names.append(state.name()+", ");
		}
		return names.substring(0, names.length()-2);
	}
}
