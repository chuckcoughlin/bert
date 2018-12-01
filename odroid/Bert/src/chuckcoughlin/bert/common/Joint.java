/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common;

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
	HEAD_Y,
	HEAD_Z,
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
	RIGHT_SHOULDER_Y
            ;
          
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
