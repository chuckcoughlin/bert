/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.bottle;

/**
 * These are the recognized commands from command controller to dispatcher.
 */
public enum RequestType
{
	GET_METRIC,              // A local property of the robot
	GET_CONFIGURATION,       // A motor property
    PLAY_STEP,
	RECORD_STEP,
	SET_CONFIGURATION,       // For a particular motor
	SET_POSE,
	GET_POSE,
	SET_STATE,               // A global motor configuration, like torque
	NONE
    ;
          
 /**
  * @return  a comma-separated list of the types in a single String.
  */
  public static String names()
  {
    StringBuffer names = new StringBuffer();
    for (RequestType type : RequestType.values())
    {
      names.append(type.name()+", ");
    }
    return names.substring(0, names.length()-2);
  }
}
