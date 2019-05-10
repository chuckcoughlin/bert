/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.message;

/**
 * These are the recognized commands from command controller to launcher.
 */
public enum RequestType
{
	COMMAND,                 // Execute a non-motor-related command
	GET_CONFIGURATION,       // A list of robot properties from the configuration file
	GET_GOALS,               // The current target settings
	GET_LIMITS,              // The EEPROM-resident limits
	GET_METRIC,              // A local property of the robot
	GET_MOTOR_PROPERTY,      // Current value of a motor property
	LIST_MOTOR_PROPERTY,     // List a property for all motors
	NOTIFICATION,            // Unsolicited message from server or parser
    PLAY_STEP,
	RECORD_STEP,
	SET_MOTOR_PROPERTY,       // For a particular motor
	SET_POSE,
	GET_POSE,
	SET_STATE,               // A global configuration, like ignoring
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
