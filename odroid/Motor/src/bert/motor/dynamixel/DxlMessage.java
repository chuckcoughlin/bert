/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.motor.dynamixel;

import java.util.Map;
import java.util.logging.Logger;

import bert.share.common.DynamixelType;
import bert.share.message.BottleConstants;
import bert.share.motor.JointProperty;

/**
 * This class contains static methods used to create and interpret different varieties \
 * of Dynamixel serial messages. Code is derived from Pypot dynamixel.v2.py and the Dynamixel
 * documentation at http://emanual.robotis.com. Applies to MX64, MX28, AX12A models.
 * The documentation is unclear about the protocol version for AX-12 models, but it appears
 * to be Protocol 2.0. We have coded on this assumption.
 */
@SuppressWarnings("unused")
public class DxlMessage  {
	private static final String CLSS = "DxlMessage";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static final byte BROADCAST_ID = (byte)0xFE; // ID to transmit to all devices connected to port
	// Constants for the instructions
	private static final byte PING = 0x01;   // Instruction that checks whether the Packet has arrived
	private static final byte READ = 0x02; 	// Instruction to read data from the Device
	private static final byte WRITE= 0x03; 	// Instruction to write data on the Device
	private static final byte REG_WRITE = 0x04; // Register the Instruction Packet to a standby status;
	private static final byte ACTION = 0x05; // Execute the Packet that was registered beforehand using REQ_WRITE
	private static final byte FACTORY_RESET = 0x06; 	// Reset the Control Table to its initial factory default settings
	private static final byte REBOOT = 0x08; 	    // Instruction to reboot the Device
	private static final byte CLEAR = 0x10; 	        // Instruction to reset certain information
	private static final byte STATUS_RETURN = 0x55; // Return Instruction for the Instruction Packet
	private static final byte SYNC_READ  = (byte)0x82; 	// For multiple devices, Instruction to read data from the same Address with the same length at once
	private static final byte SYNC_WRITE = (byte)0x83; // For multiple devices, Instruction to write data on the same Address with the same length at once
	private static final byte BULK_READ  = (byte)0x92; // For multiple devices, Instruction to read data from different Addresses with different lengths at once
	private static final byte BULK_WRITE = (byte)0x93; 
	
	/**
	 * Create a serial message to broadcast a ping request to all motors.
	 * This is taken directly from http://emanual.robotis.com/docs/en/dxl/protocol1/
	 * @return byte array for message
	 */
	public static byte[] bytesToBroadcastPing() {
		int length = 2;  // Remaining bytes past length including checksum
		byte[] bytes = new byte[length+4];  // Account for header and length
		setHeader(bytes,BROADCAST_ID);
		bytes[3] = (byte)length; 
		bytes[4] = PING;
		setChecksum(bytes);
		return bytes;
	}
	
	/**
	 * Create a serial message to read a specified property of a motor.
	 * @param id of the motor
	 * @param propertyName the name of the desired property (must be a joint property)
	 * @return byte array with command to read the property
	 */
	public static byte[] bytesToGetProperty(int id,String propertyName) {
		int length = 4;  // Remaining bytes past length including checksum
		byte[] bytes = new byte[length+4];  // Account for header and length
		setHeader(bytes,id);
		bytes[3] = (byte)length; 
		bytes[4] = READ;
		bytes[5] = DxlConversions.addressForPresentProperty(propertyName);
		bytes[6] = DxlConversions.dataBytesForProperty(propertyName);
		setChecksum(bytes);
		return bytes;
	}
	
	/**
	 * Create a broadcast message to return the present status of a specified property
	 * for of all motors.
	 * @param propertyName the name of the desired property (must be a joint property)
	 * @return byte array with broadcast command to read the property 
	 */
	public static byte[] bytesToListProperty(String propertyName) {
		int length = 4;  // Remaining bytes past length including checksum
		byte[] bytes = new byte[length+4];  // Account for header and length
		setHeader(bytes,BROADCAST_ID);
		bytes[3] = (byte)length; 
		bytes[4] = READ;
		bytes[5] = DxlConversions.addressForPresentProperty(propertyName);
		bytes[6] = DxlConversions.dataBytesForProperty(propertyName);
		setChecksum(bytes);
		return bytes;
	}
	
	/**
	 * Create a serial message to set the goal speed of a motor
	 * @param id of the motor
	 * @param speed in degrees/sec
	 * @return byte array with command to set speed
	 */
	public static byte[] bytesToSetSpeed(int id,DynamixelType model,double speed) {
		int dxlSpeed = DxlConversions.speedToDxl(model, speed);
		int length = 7;  // Remaining bytes past length including crc
		byte[] bytes = new byte[length+7];  // Account for header and length
		setHeader(bytes,id);
		bytes[5] = (byte)length; 
		bytes[6] = 0;
		bytes[7] = WRITE;
		bytes[8] = 0x20;    // Moving speed
		bytes[9] = 0;
		bytes[10] = (byte)(dxlSpeed >>8);
		bytes[11] = (byte)(dxlSpeed & 0xFF);
		setChecksum(bytes);
		return bytes;
	}
	
	/**
	 * Create a serial message to set the torque limit for a motor
	 * @param id of the motor
	 * @param goal in n/m
	 * @return byte array with command to set speed
	 */
	public static byte[] bytesToSetTorque(int id,DynamixelType model,double goal) {
		int dxlTorque = DxlConversions.torqueToDxl(model, goal);
		int length = 7;  // Remaining bytes past length including crc
		byte[] bytes = new byte[length+7];  // Account for header and length
		setHeader(bytes,id); 
		bytes[5] = (byte)length; 
		bytes[6] = 0;
		bytes[7] = WRITE;
		bytes[8] = 0x22;    // Torque Goal
		bytes[9] = 0;
		bytes[10] = (byte)(dxlTorque >>8);
		bytes[11] = (byte)(dxlTorque & 0xFF);

		setChecksum(bytes);
		return bytes;
	}
	
	/**
	 * Create a string suitable for printing and debugging.
	 * @param bytes
	 * @return a formatted string of the bytes as hex digits.
	 */
	public static String dump(byte[] bytes) {
		StringBuffer sb = new StringBuffer();
		int index = 0;
		while(index<bytes.length) {
			//if( bytes[index]=='\0') break;
			sb.append(String.format("%02X",bytes[index]));
			sb.append(" ");
			index++;
		}
		
		// Add the buffer length
		sb.append("(");
		sb.append(bytes.length);
		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * Analyze a response buffer for position of a motor. Augment the
	 * supplied Properties with the result (possibly an error).
	 * @param props properties from a MessageBottle
	 * @param bytes status response from the controller
	 * @return the position from the status response
	 */
	public static void updateParameterFromBytes(String parameterName,Map<String,String> props,byte[] bytes) {
		String msg = "";
		if( verifyHeader(bytes) ) {
			msg = String.format("%s.updateParameterFromBytes: %s",CLSS,dump(bytes));
			LOGGER.info(msg);
		
			int id = bytes[2];
			int err= bytes[4];
			int param= bytes[5] + 256*bytes[6];
			if( err==0 ) {
				props.put(BottleConstants.PROPERTY_NAME,parameterName);
					props.put(parameterName, String.valueOf(param));
			}
			else {
				msg = String.format("%s.updateParameterFromBytes: message returned error %d (%s)",CLSS,err,dump(bytes));
				props.put(BottleConstants.ERROR, msg);
				LOGGER.severe(msg);
			}
		}
		else {
			msg = String.format("%s.updateParameterFromBytes: Illegal message: %s",CLSS,dump(bytes));
			props.put(BottleConstants.ERROR, msg);
			LOGGER.severe(msg);
		}
	}
	
	/**
	 * Analyze the response buffer for motor positions (probably only one) and
	 * update the supplied position map accordingly. Log errors, there's not much
	 * else we can do.
	 * @param parameterName the name of the Joint property being handled
	 * @param bytes status response from the controller
	 * @param positions an array of positions by id, supplied.
	 */
	public static void updateParameterArrayFromBytes(String parameterName, Map<Integer,String> parameters,byte[] bytes) {
		String msg = "";
		if( verifyHeader(bytes) ) {
			int id = bytes[2];
			int err= bytes[4];
			int param= bytes[5] + 256*bytes[6];
			if( err==0 ) {
				parameters.put(id, String.valueOf(param));
			}
			else {
				msg = String.format("%s.updatePositionFromBytes: message returned error %d (%s)",CLSS,err,dump(bytes));
				LOGGER.severe(msg);
			}
		}
		else {
			LOGGER.severe(String.format("%s.updatePositionArrayFromBytes: Header not found: %s",CLSS,dump(bytes)));
			
		}
	}
	// ===================================== Private Methods =====================================
	// Set the header up until the length field. The header includes the device ID.
	// Protocol 1. 3 bytes
	private static void setHeader(byte[] bytes, int id) {
		bytes[0] = (byte)0xFF;
		bytes[1] = (byte)0xFF;
		bytes[2] = (byte) id;
	}
	
	/**
	 * Consider bytes 0-(len-2), then insert into last bytes. "oversize" variables
	 * to avoid problem with no "unsigned" in Java. Ultimately we discard all except
	 * low order bits.
	 * @see http://emanual.robotis.com/docs/en/dxl/protocol1/
	 * @param buf the byte buffer
	 */
	public static void setChecksum( byte[] buf ) {
		int size = buf.length - 1;   // Exclude bytes to hold Checksum
		int sum = 0;    // Instruction checksum.
	    for( int j=2; j < size; j++ ) {
	    	sum = sum+buf[j];
	    }
	    sum = sum&0xFF;
	    buf[size]   =  (byte)(255-sum);
	}

	
	/**
	 * Protocol 1
	 */
	private static boolean verifyHeader(byte[] bytes) {
		boolean result = false;
		if( bytes.length > 6     &&
			bytes[0]==(byte)0xFF &&
			bytes[1]==(byte)0xFF  
		  ) {
			
			result = true;
		}
		return result;
	}

	
	/**
	 * Test using example in Robotis documentation for WRITE command and status, 5.3.3.2 and 5.3.3.3.
	 * http://emanual.robotis.com/docs/en/dxl/protocol2
	 */
	public static void main(String [] args) {
		// Protocol 1
		byte[] bytes = new byte[7];
		setHeader(bytes,0x01);
		bytes[3] = 4;    // Bytes past this field.
		bytes[4] = READ;
		bytes[5] = 0x2B;
		bytes[6] = 0x1;
		setChecksum(bytes);
		// Should be CC
        System.out.println("READ  with checksum: "+dump(bytes));
        
        // Protocol 1
        bytes = bytesToBroadcastPing();
        // Checksum should be FE
        System.out.println("PING (1)  with checksum: "+dump(bytes));


    }
}
