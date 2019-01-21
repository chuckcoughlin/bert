/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.share.bottle;


/**
 *  Define strings used in requests and responses between server and client.
 */
public interface BottleConstants   {   
	
	// Well-known properties that define the request/response syntax
	public final static String PROPERTY_ERROR      = "error";      // The last request resulted in an error
	public final static String PROPERTY_JOINT      = "joint";      // Value is a Joint name
	public final static String PROPERTY_METRIC     = "metric";     // Value is a MetricType
	public final static String PROPERTY_PROPERTY   = "property";   // Property to set or get, a JointProperty
	public final static String PROPERTY_RESPONSE   = "response";   // Value is a ResponseType
	public final static String PROPERTY_REQUEST    = "request";    // Value is a RequestType
	public final static String PROPERTY_SOURCE     = "source";     // Source of message, ControllerType
	
	// Properties appropriate for responses
	public final static String TEXT     = "text";     // End-user appropriate text result
	public final static String VALUE    = "value";    // Single-value result
	
	
	
	public final static String POSE_NAME           = "pose";

	
	
	// Command names
	public final static String COMMAND_FREEZE      = "freeze";
	public final static String COMMAND_RELAX       = "relax";
	public final static String COMMAND_WAKE        = "wake";
	
}
