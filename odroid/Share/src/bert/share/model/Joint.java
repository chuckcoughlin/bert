/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.model;

/**
 * These are the canonical names for the joints of the humanoid.
 */
public enum Joint
{
	ABS_X,
	ABS_Y,
	ABS_Z,
	BUST_X,
	BUST_Y,
	NECK_Y,
	NECK_Z,
	LEFT_ANKLE_Y,
	LEFT_ARM_Z,
	LEFT_ELBOW_Y,
	LEFT_HIP_X,
	LEFT_HIP_Y,
	LEFT_HIP_Z,
	LEFT_KNEE_Y,
	LEFT_SHOULDER_X,
	LEFT_SHOULDER_Y,
	RIGHT_ANKLE_Y,
	RIGHT_ARM_Z,
	RIGHT_ELBOW_Y,
	RIGHT_HIP_X,
	RIGHT_HIP_Y,
	RIGHT_HIP_Z,
	RIGHT_KNEE_Y,
	RIGHT_SHOULDER_X,
	RIGHT_SHOULDER_Y,
	UNKNOWN
	;

	/**
	 * Convert the Joint enumeration to text that can be pronounced.
	 * @param joint the enumeration
	 * @return user-recognizable text
	 */
	public static String toText(Joint joint) {
		String text = "";
		switch( joint ) {
			case ABS_X: text = "abdomen x"; break;
			case ABS_Y: text = "abdomen y"; break;
			case ABS_Z: text = "abdomen z"; break;
			case BUST_X: text = "chest horizontal"; break;
			case BUST_Y: text = "chest vertical"; break;
			case NECK_Y: text = "neck y"; break;
			case NECK_Z: text = "neck z"; break;
			case LEFT_ANKLE_Y: text = "left ankle"; break;
			case LEFT_ARM_Z:   text = "left arm z"; break;
			case LEFT_ELBOW_Y: text = "left elbow"; break;
			case LEFT_HIP_X:   text = "left hip x"; break;
			case LEFT_HIP_Y:   text = "left hip y"; break;
			case LEFT_HIP_Z:   text = "left hip z"; break;
			case LEFT_KNEE_Y:  text = "left knee"; break;
			case LEFT_SHOULDER_X: text = "left shoulder horizontal"; break;
			case LEFT_SHOULDER_Y: text = "left shoulder vertical"; break;
			case RIGHT_ANKLE_Y: text = "right ankle"; break;
			case RIGHT_ARM_Z:   text = "right arm z"; break;
			case RIGHT_ELBOW_Y:  text = "right elbow"; break;
			case RIGHT_HIP_X:    text = "right hip x"; break;
			case RIGHT_HIP_Y:    text = "right hip y"; break;
			case RIGHT_HIP_Z:    text = "right hip z"; break;
			case RIGHT_KNEE_Y:   text = "right knee"; break;
			case RIGHT_SHOULDER_X: text = "right shoulder horizontal"; break;
			case RIGHT_SHOULDER_Y: text = "right shoulder vertical"; break;
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
		for (Joint type : Joint.values())
		{
			names.append(type.name()+", ");
		}
		return names.substring(0, names.length()-2);
	}
}
