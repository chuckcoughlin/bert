/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.model;

/**
 * These are the canonical names for the linka of the humanoid.
 */
public enum Link
{
	LEFT_FOOT,
	PELVIS,
	UNKNOWN
	;

	/**
	 * Convert the Link enumeration to text that can be pronounced.
	 * @param joint the enumeration
	 * @return user-recognizable text
	 */
	public static String toText(Link link) {
		String text = "";
		switch( link ) {
			case LEFT_FOOT: text = "left foot"; break;
			case PELVIS: text = "pelvis"; break;
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
		for (Link type : Link.values())
		{
			names.append(type.name()+", ");
		}
		return names.substring(0, names.length()-2);
	}
}
