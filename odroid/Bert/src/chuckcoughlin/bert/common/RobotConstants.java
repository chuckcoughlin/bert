/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 *   The block controller is designed to be called from the client
 *   via RPC. All methods must be thread safe,
 */
package chuckcoughlin.bert.common;



/**
 *  List
 */
public interface RobotConstants   {   
	public final static String MODULE_ID = "block";     // See module-blt.xml
	public final static String MODULE_NAME = "BLT";     // See build-blt.xml
	public final static String SFC_MODULE_ID = "com.ils.sfc"; 

		
	// These are the key names allowed in the Python dictionary that defines a block attribute.
	public static final String BLOCK_ATTRIBUTE_BINDING    = "binding";
	
}
