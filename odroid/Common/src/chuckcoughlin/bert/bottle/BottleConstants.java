/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package chuckcoughlin.bert.bottle;


/**
 *  Define strings used in requests and responses between server and client.
 */
public interface BottleConstants   {   
   
	// Commands handled by the server
	public final static String COMMAND_MOVE     = "move";       // Drive Dynamixel motors to new positions 
	public final static String COMMAND_PROPERTY = "property";   // Request the value of a property
	
	// Status replies
	public final static String STATUS_OK        = "ok";         // Success
	
	// Properties available from the server.
	public final static String PROPERTY_CADENCE = "cadence";
	public final static String PROPERTY_NAME = "name";
}
