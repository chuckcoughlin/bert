/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.control;

/**
 * These are the canonical names for appendages on various limbs.
 * These are body parts where we need locations.
 */
public enum Appendage
{
	LEFT_EAR,
	LEFT_EYE,
	LEFT_FINGER,
	LEFT_HEEL,
	LEFT_TOE,
	NOSE,
	RIGHT_EAR,
	RIGHT_EYE,
	RIGHT_FINGER,
	RIGHT_HEEL,
	RIGHT_TOE,
	UNKNOWN
	;

	/**
	 * Convert the Limb enumeration to text that can be pronounced.
	 * @param limb the enumeration
	 * @return user-recognizable text
	 */
	public static String toText(Appendage limb) {
		String text = "";
		switch( limb ) {
			case LEFT_EAR: text = "left ear"; break;
			case LEFT_EYE: text = "left eye"; break;
			case LEFT_FINGER: text = "left finger"; break;
			case LEFT_HEEL: text = "left heel"; break;
			case LEFT_TOE: text = "left toe"; break;
			case NOSE: text = "nose"; break;
			case RIGHT_EAR: text = "right ear"; break;
			case RIGHT_EYE: text = "right eye"; break;
			case RIGHT_FINGER: text = "right finger"; break;
			case RIGHT_HEEL: text = "right heel"; break;
			case RIGHT_TOE: text = "right toe"; break;
			case UNKNOWN: text = "unknown"; break;
		}
		return text;
	}
	/**
	 * @return  a comma-separated list of all block states in a single String.
	 */
	public static String names()
	{
		StringBuffer names = new StringBuffer();
		for (Appendage type : Appendage.values())
		{
			names.append(type.name()+", ");
		}
		return names.substring(0, names.length()-2);
	}
}
