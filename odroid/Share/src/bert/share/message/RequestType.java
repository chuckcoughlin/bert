/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.message;

/**
 * These are the recognized commands from command controller to dispatcher.
 */
public enum RequestType
{
	COMMAND,                 // Execute a non-motor-related command
	GET_METRIC,              // A local property of the robot
	GET_METRICS,             // A list of local properties of the robot
	GET_MOTOR_PROPERTY,      // A motor property
	LIST_MOTOR_PROPERTY,     // List a property for all motors
	NOTIFICATION,            // Unsolicited message from server
    PLAY_STEP,
	RECORD_STEP,
	SET_MOTOR_PROPERTY,       // For a particular motor
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
