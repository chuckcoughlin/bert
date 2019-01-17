/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.share.bottle;


/**
 *  Define strings used in requests and responses between server and client.
 */
public interface BottleConstants   {   
	
	// Propperties that define the request/response syntax
	public final static String POSE_NAME           = "pose";
	public final static String PROPERTY_COMMAND    = "command";  
	public final static String PROPERTY_ERROR      = "error";      // The last request resulted in an error
	public final static String PROPERTY_NAME       = "name";
	public final static String PROPERTY_PROMPT     = "prompt";
	public final static String PROPERTY_RESPONSE   = "response";   // Value is a ResponseType
	public final static String PROPERTY_REQUEST    = "request";    // Value is a RequestType
	public final static String PROPERTY_SOURCE     = "source";     // Source of message, controller type
	
	public final static String STATE_NAME          = "state";
	
	// Command names
	public final static String COMMAND_FREEZE      = "freeze";
	public final static String COMMAND_RELAX       = "relax";
	public final static String COMMAND_WAKE        = "wake";
		
	// Valus of properties
	public final static String VALUE_CADENCE       = "cadence";
	

	
	// Key for a message that signals an error
	public final static String ERROR__MESSAGE    = "error";
}
