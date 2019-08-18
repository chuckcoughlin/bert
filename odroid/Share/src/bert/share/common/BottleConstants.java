/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.share.common;


/**
 *  Define strings used in requests and responses between server and client.
 */
public interface BottleConstants   {   
	
	// Well-known keys for the properties map inside a request/response
	// Additional data values as appropriate are keyed with the JointProperty keys
	public final static String APPENDAGE_NAME      = "appendage";  // Request applies to this appendage, value is a Appendage
	public final static String COMMAND_NAME        = "command";    // Value is a well-known command name, see below
	public final static String ERROR               = "error";      // Request resulted in an error, value is error text
	public final static String JOINT_NAME          = "joint";      // Request applies to this joint, value is a Joint
	public final static String LIMB_NAME           = "limb";       // Request applies to this limb, value is a Limb
	public final static String METRIC_NAME         = "metric";     // Value is a MetricType
	public final static String POSE_NAME           = "pose";       // Name of a pose, must exist in database
	public final static String PROPERTY_NAME       = "property";   // Value is a JointProperty. Subject of original request.
	public final static String TYPE                = "type";       // Type of request, a RequestType
	public final static String SOURCE              = "source";     // Original source of request, value is a HandlerType
	public final static String TEXT                = "text";       // End-user appropriate text result
	
	// Command names
	public final static String COMMAND_FREEZE      = "freeze";
	public final static String COMMAND_HALT        = "halt";
	public final static String COMMAND_RELAX       = "relax";
	public final static String COMMAND_RESET       = "reset";
	public final static String COMMAND_SHUTDOWN    = "shutdown";
	public final static String COMMAND_WAKE        = "wake";
	
	// Pose names (these are required to exist)
	public final static String POSE_HOME           = "home";
	public final static String POSE_NORMAL_SPEED   = "normal speed";
	
	// Message from tablet
	public final static int HEADER_LENGTH = 4;     // Includes semi-colon
	
	// Nominal values for torque and speed in percent
	public final static int SPEED_NORMAL         = 20;
	public final static int TORQUE_NORMAL        = 20;
	// For values that are boolean. Use these strings for "values"
	public final static String ON_VALUE       = "1";
	public final static String OFF_VALUE      = "0";

}
