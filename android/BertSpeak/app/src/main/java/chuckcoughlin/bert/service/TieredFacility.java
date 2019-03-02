/**
 *   (c) 2019  Charles Coughlin. All rights reserved.
 *   (MIT License)
 */
package chuckcoughlin.bert.service;

/**
 * These are the facilities supported by the VoiceService. These
 * are ordered in that each is dependent on the previous.
 */
public enum TieredFacility {
	BLUETOOTH,
    SOCKET,
    VOICE
	;

	/**
	 * @return  a comma-separated list of all facilities in a single String.
	 */
	public static String names() {
		StringBuffer names = new StringBuffer();
		for (TieredFacility state : TieredFacility.values())
		{
			names.append(state.name()+", ");
		}
		return names.substring(0, names.length()-2);
	}
}
