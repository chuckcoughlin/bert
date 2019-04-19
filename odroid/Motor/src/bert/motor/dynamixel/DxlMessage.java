/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.motor.dynamixel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import bert.share.common.DynamixelType;
import bert.share.message.BottleConstants;
import bert.share.motor.JointProperty;
import bert.share.motor.MotorConfiguration;

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
	 * Create a serial message to read the current goals of a particular motor.
	 * @param id of the motor
	 * @return byte array with command to read the block of RAM
	 */
	public static byte[] bytesToGetGoals(int id) {
		int length = 4;  // Remaining bytes past length including checksum
		byte[] bytes = new byte[length+4];  // Account for header and length
		setHeader(bytes,id);
		bytes[3] = (byte)length; 
		bytes[4] = READ;
		bytes[5] = DxlConversions.GOAL_BLOCK_ADDRESS;
		bytes[6] = DxlConversions.GOAL_BLOCK_BYTES;
		setChecksum(bytes);
		return bytes;
	}
	/**
	 * Create a serial message to read the block of limits contained in EEPROM
	 * for a particular motor.
	 * @param id of the motor
	 * @return byte array with command to read the block of EEPROM
	 */
	public static byte[] bytesToGetLimits(int id) {
		int length = 4;  // Remaining bytes past length including checksum
		byte[] bytes = new byte[length+4];  // Account for header and length
		setHeader(bytes,id);
		bytes[3] = (byte)length; 
		bytes[4] = READ;
		bytes[5] = DxlConversions.LIMIT_BLOCK_ADDRESS;
		bytes[6] = DxlConversions.LIMIT_BLOCK_BYTES;
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
	 * Create a bulk read message to interrogate a list of motors for a specified
	 * property. Unfortunately AX-12 motors do not support this request, so must be
	 * queried separately (thus the list). Note that the bulk read results in individual
	 * responses from each motor.
	 * @param propertyName the name of the desired property (must be a joint property)
	 * @return list of byte arrays with bulk read plus extras for any AX-12. 
	 */
	public static List<byte[]> byteArrayListToListProperty(String propertyName,Collection<MotorConfiguration> configurations) {
		List<byte[]> messages = new ArrayList<>();
		int count = configurations.size();   // Number of motors, less AX-12
		for( MotorConfiguration mc:configurations) {
			if(mc.getType().equals(DynamixelType.AX12) ) {
				int length = 4;  // Remaining bytes past length including checksum
				byte[] bytes = new byte[length+4];  // Account for header and length
				setHeader(bytes,mc.getId());
				bytes[3] = (byte)length; 
				bytes[4] = READ;
				bytes[5] = DxlConversions.addressForPresentProperty(propertyName);
				bytes[6] = DxlConversions.dataBytesForProperty(propertyName);
				setChecksum(bytes);
				messages.add(bytes);
				count--;
			}
		}
		
		// Now lay out the bulk read message for everyone else.
		int length = 3*count+3;  // Remaining bytes past length including checksum
		byte[] bytes = new byte[length+4];  // Account for header and length
		setHeader(bytes,BROADCAST_ID);
		bytes[3] = (byte)length; 
		bytes[4] = BULK_READ;
		bytes[5] = 0;
		int addr = 6;
		for(MotorConfiguration mc:configurations) {
			if(mc.getType().equals(DynamixelType.AX12) ) continue;
			bytes[addr] = DxlConversions.dataBytesForProperty(propertyName);
			bytes[addr+1] = (byte)mc.getId();
			bytes[addr+2] = DxlConversions.addressForPresentProperty(propertyName);
			addr += 3;
		}
		setChecksum(bytes);
		messages.add(bytes);
		return messages;
	}
	
	/**
	 * Create a serial message to set the goal speed of a motor
	 * @param id of the motor
	 * @param speed in degrees/sec
	 * @return byte array with command to set speed
	 */
	public static byte[] bytesToSetSpeed(int id,DynamixelType model,boolean isDirect,double speed) {
		int dxlSpeed = DxlConversions.speedToDxl(model, isDirect, speed);
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
	public static byte[] bytesToSetTorque(int id,DynamixelType model,boolean isDirect,double goal) {
		int dxlTorque = DxlConversions.torqueToDxl(model,isDirect,goal);
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
	 * Analyze a response buffer returned from a request for goal values for a motor. Goals
	 * parameters are: position, speed, torque. Results will be entered in the properties map.
	 * Use the absolute value for speeds and torques.
	 * @param type the model of the motor
	 * @param isDirect the orientation of the motor
	 * @param props properties from a MessageBottle
	 * @param bytes status response from the controller
	 */
	public static void updateGoalsFromBytes(DynamixelType type,boolean isDirect,Map<String,String> props,byte[] bytes) {
		String msg = "";
		if( verifyHeader(bytes) ) {
			msg = String.format("%s.updateGoalsFromBytes: %s",CLSS,dump(bytes));
		
			int id = bytes[2];
			int err= bytes[4];
			
			String parameterName = JointProperty.POSITION.name();
			double v1 = DxlConversions.valueForProperty(parameterName,type,isDirect,bytes[5],bytes[6]);
			String t1  = DxlConversions.textForProperty(parameterName,type,isDirect,bytes[5],bytes[6]);
			props.put(parameterName,String.valueOf(v1));
			
			parameterName = JointProperty.SPEED.name();    // Non-directional
			double v2 = DxlConversions.valueForProperty(parameterName,type,true,bytes[7],bytes[8]);
			String t2  = DxlConversions.textForProperty(parameterName,type,true,bytes[7],bytes[8]);
			props.put(parameterName,String.valueOf(v2));
			
			parameterName = JointProperty.TORQUE.name();   // Non-directional
			double v3 = DxlConversions.valueForProperty(parameterName,type,true,bytes[9],bytes[10]);
			String t3  = DxlConversions.textForProperty(parameterName,type,true,bytes[9],bytes[10]);
			props.put(parameterName,String.valueOf(v3));
			
			String text = String.format("Goal position, speed and torque are : %s, %s, %s", t1,t2,t3);
			if( err==0 ) {
				props.put(BottleConstants.TEXT,text);	
			}
			else {
				msg = String.format("%s.updateGoalsFromBytes: message returned error %d (%s)",CLSS,err,dump(bytes));
				props.put(BottleConstants.ERROR, msg);
				LOGGER.severe(msg);
			}
		}
		else {
			msg = String.format("%s.updateGoalsFromBytes: Illegal message: %s",CLSS,dump(bytes));
			props.put(BottleConstants.ERROR, msg);
			LOGGER.severe(msg);
		}
	}
	/**
	 * Analyze a response buffer returned from a request for EEPROM limits for a motor. Limit
	 * parameters are: angles, temperature, voltage and torque. Of these we extract only the
	 * angles and torque. Results will be entered in the properties map.
	 * @param type the model of the motor
	 * @param isDirect the orientation of the motor
	 * @param props properties from a MessageBottle
	 * @param bytes status response from the controller
	 */
	public static void updateLimitsFromBytes(DynamixelType type,boolean isDirect,Map<String,String> props,byte[] bytes) {
		String msg = "";
		if( verifyHeader(bytes) ) {
			msg = String.format("%s.updateLimitsFromBytes: %s",CLSS,dump(bytes));
		
			int id = bytes[2];
			int err= bytes[4];
			
			String parameterName = JointProperty.MAXIMUMANGLE.name(); // CW
			double v1 = DxlConversions.valueForProperty(parameterName,type,isDirect,bytes[5],bytes[6]);
			String t1  = DxlConversions.textForProperty(parameterName,type,isDirect,bytes[5],bytes[6]);
			props.put(parameterName,String.valueOf(v1));
			
			parameterName = JointProperty.MINIMUMANGLE.name();   // CCW
			double v2 = DxlConversions.valueForProperty(parameterName,type,isDirect,bytes[7],bytes[8]);
			String t2  = DxlConversions.textForProperty(parameterName,type,isDirect,bytes[7],bytes[8]);
			props.put(parameterName,String.valueOf(v2));
			
			parameterName = JointProperty.TORQUE.name();    // Non-directional
			double v3 = DxlConversions.valueForProperty(parameterName,type,true,bytes[12],bytes[13]);
			String t3  = DxlConversions.textForProperty(parameterName,type,true,bytes[12],bytes[13]);
			props.put(parameterName,String.valueOf(v3));
			
			String text = String.format("Limiting angles and torque are : %s, %s, %s", t1,t2,t3);
			if( err==0 ) {
				props.put(BottleConstants.TEXT,text);	
			}
			else {
				msg = String.format("%s.updateLimitsFromBytes: message returned error %d (%s)",CLSS,err,dump(bytes));
				props.put(BottleConstants.ERROR, msg);
				LOGGER.severe(msg);
			}
		}
		else {
			msg = String.format("%s.updateLimitsFromBytes: Illegal message: %s",CLSS,dump(bytes));
			props.put(BottleConstants.ERROR, msg);
			LOGGER.severe(msg);
		}
	}
	/**
	 * Analyze a response buffer for some parameter of a motor. Augment the
	 * supplied Properties with the result (possibly an error).
	 * @param parameterName the requested parameter
	 * @param type the model of the motor
	 * @param isDirect the orientation of the motor
	 * @param props properties from a MessageBottle
	 * @param bytes status response from the controller
	 */
	public static void updateParameterFromBytes(String parameterName,DynamixelType type,boolean isDirect,Map<String,String> props,byte[] bytes) {
		String msg = "";
		if( verifyHeader(bytes) ) {
			msg = String.format("%s.updateParameterFromBytes: %s",CLSS,dump(bytes));
		
			int id = bytes[2];
			int err= bytes[4];
			
			double value = DxlConversions.valueForProperty(parameterName,type,isDirect,bytes[5],bytes[6]);
			String text = DxlConversions.textForProperty(parameterName,type,isDirect,bytes[5],bytes[6]);
			if( err==0 ) {
				props.put(BottleConstants.PROPERTY_NAME,parameterName);
				props.put(BottleConstants.TEXT,text);
				props.put(parameterName,String.valueOf(value));
				
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
	 * Analyze the response buffer for the indicated motor parameter and
	 * update the supplied position map accordingly. There may be several responses concatenated.
	 * We assume that the parameter is 2 bytes. Log errors, there's not much else we can do.
	 * @param parameterName the name of the Joint property being handled
	 * @param configurations a map of motor configurations by id
	 * @param bytes status response from the controller
	 * @param parameters an array of positions by id, supplied. This is augmented by the method.
	 */
	public static void updateParameterArrayFromBytes(String parameterName, Map<Integer,MotorConfiguration> configurations,byte[] bytes,Map<Integer,String> parameters) {
		String msg = "";
		int length = 7;
		int index  = 0;
		while( index<bytes.length ) {
			if( verifyHeader(bytes,index) ) {
				int id = bytes[index+2];
				length = bytes[index+3] + 4;  // Takes care of fixed bytes pre-length
				int err= bytes[index+4];
				MotorConfiguration mc =  configurations.get(id);
				if( err==0 && mc!=null ) {
					DynamixelType type = mc.getType();
					boolean isDirect   = mc.isDirect();
					double param= DxlConversions.valueForProperty(parameterName,type,isDirect,bytes[index+5],bytes[index+6]);
					parameters.put(id, String.valueOf(param));
				}
				else if(err!=0){
					msg = String.format("%s.updateParameterArrayFromBytes: motor %d returned error %d (%s)",CLSS,id,err,dump(bytes));
					LOGGER.severe(msg);
				}
				// mc = null
				else {
					msg = String.format("%s.updateParameterArrayFromBytes: id of %d not supplied in motor configurations",CLSS,id,dump(bytes));
					LOGGER.severe(msg);
				}
			}
			else {
				LOGGER.severe(String.format("%s.updateParameterArrayFromBytes: Header not found: %s",CLSS,dump(bytes)));

			}
			index = index+length;
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
		if( bytes.length > 5     &&
			bytes[0]==(byte)0xFF &&
			bytes[1]==(byte)0xFF  
		  ) {
			
			result = true;
		}
		return result;
	}

	private static boolean verifyHeader(byte[] bytes,int index) {
		boolean result = false;
		if( bytes.length > index+5     &&
			bytes[index]  ==(byte)0xFF &&
			bytes[index+1]==(byte)0xFF  
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
