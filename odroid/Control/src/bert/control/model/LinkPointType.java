/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.model;

/**
 * These are the possible types of "LinkPoints" or
 * connection locations on a link.
 */
public enum LinkPointType
{
	APPENDAGE,
	ORIGIN,
	REVOLUTE
	;


	/**
	 * @return  a comma-separated list of all block states in a single String.
	 */
	public static String names()
	{
		StringBuffer names = new StringBuffer();
		for (LinkPointType type : LinkPointType.values())
		{
			names.append(type.name()+", ");
		}
		return names.substring(0, names.length()-2);
	}
}
