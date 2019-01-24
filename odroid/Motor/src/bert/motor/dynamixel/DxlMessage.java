/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.motor.dynamixel;

import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.Properties;

import bert.share.bottle.BottleConstants;
import bert.share.common.DynamixelType;

/**
 * This class contains static methods used to create and interpret different varieties \
 * of Dynamixel serial messages. Code is derived from Pypot dynamixel.v2.py and the Dynamixel
 * documentation at http://emanual.robotis.com. Applies to MX64, MX28, AX12A models.
 * 
 * Protocol 2.0.
 */
@SuppressWarnings("unused")
public class DxlMessage  {
	private static final String CLSS = "DxlMessage";
	private static System.Logger LOGGER = System.getLogger(CLSS);
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
	private static int[] crc_table = {
			0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241,
            0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1, 0xC481, 0x0440,
            0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40,
            0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841,
            0xD801, 0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40,
            0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41,
            0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701, 0x17C0, 0x1680, 0xD641,
            0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040,
            0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240,
            0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441,
            0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41,
            0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840,
            0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41,
            0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81, 0x2C40,
            0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640,
            0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041,
            0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240,
            0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441,
            0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41,
            0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840,
            0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41,
            0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40,
            0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640,
            0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041,
            0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241,
            0x9601, 0x56C0, 0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440,
            0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40,
            0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880, 0x9841,
            0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40,
            0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41,
            0x4400, 0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641,
            0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040
	};
	/**
	 * Create a serial message to read the current position of a motor specific
	 * @param id of the motor
	 * @return byte array with command to read the position
	 */
	public static byte[] bytesToGetPosition(int id) {
		int length = 7;  // Remaining bytes past length including crc
		byte[] bytes = new byte[length+7];  // Account for header and length
		setHeader(bytes);
		bytes[4] = (byte)id; 
		bytes[5] = (byte)length; 
		bytes[6] = 0;
		bytes[7] = READ;
		bytes[8] = 0x24;    // Current position
		bytes[9] = 0;
		bytes[10] = 0x04;   // 4 bytes
		bytes[11] = 0;
		setCrc(bytes);
		return bytes;
	}
	
	/**
	 * Create a serial message to set the speed of a motor
	 * @param id of the motor
	 * @param speed in degrees/sec
	 * @return byte array with command to set speed
	 */
	public static byte[] bytesToSetSpeed(int id,DynamixelType model,double speed) {
		int dxlSpeed = DxlConversions.speedToDxl(model, speed);
		int length = 7;  // Remaining bytes past length including crc
		byte[] bytes = new byte[length+7];  // Account for header and length
		setHeader(bytes);
		bytes[4] = (byte)id; 
		bytes[5] = (byte)length; 
		bytes[6] = 0;
		bytes[7] = WRITE;
		bytes[8] = 0x20;    // Moving speed
		bytes[9] = 0;
		bytes[10] = (byte)(dxlSpeed >>8);
		bytes[11] = (byte)(dxlSpeed & 0xFF);
		setCrc(bytes);
		return bytes;
	}
	
	/**
	 * Create a serial message to set the torque goal for a motor
	 * @param id of the motor
	 * @param goal in n/m
	 * @return byte array with command to set speed
	 */
	public static byte[] bytesToSetTorque(int id,DynamixelType model,double goal) {
		int dxlTorque = DxlConversions.torqueToDxl(model, goal);
		int length = 7;  // Remaining bytes past length including crc
		byte[] bytes = new byte[length+7];  // Account for header and length
		setHeader(bytes);
		bytes[4] = (byte)id; 
		bytes[5] = (byte)length; 
		bytes[6] = 0;
		bytes[7] = WRITE;
		bytes[8] = 0x22;    // Torque Goal
		bytes[9] = 0;
		bytes[10] = (byte)(dxlTorque >>8);
		bytes[11] = (byte)(dxlTorque & 0xFF);
		setCrc(bytes);
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
			sb.append(String.format("%02x",bytes[index]));
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
	 * Analyze a response buffer for position of a motor. Update the
	 * supplied Properties with the result (possibly an error).
	 * @param id of the motor
	 * @return byte array with command to read the position
	 */
	public static void updatePositionFromBytes(byte[] bytes,Properties props) {
		int length = 7;  // Remaining bytes past length including crc
		if( bytes.length<length ) {
			String msg = String.format("%s.updatePositionFromBytes: Message too short: %s",CLSS,dump(bytes));
			props.setProperty(BottleConstants.PROPERTY_ERROR, msg);
		}
		else if( !verifyHeader(bytes) ) {
			String msg = String.format("%s.updatePositionFromBytes: Header not found: %s",CLSS,dump(bytes));
			props.setProperty(BottleConstants.PROPERTY_ERROR, msg);
		}
		else {
			String msg = String.format("%s.updatePositionFromBytes: Unimplemented %s",CLSS,dump(bytes));
			LOGGER.log(Level.INFO,msg);
		}
	}
	
	/**
	 * Analyze the response buffer for motor positions (probebly only one) and
	 * update the supplied position map accordingly. Log errors, there's not much
	 * else we can do.
	 * @param bytes status response from the controller
	 * @param positions an array of positions by id, supplied.
	 */
	public static void updatePositionArrayFromBytes(byte[] bytes,Map<Integer,Integer> positions) {
		int length = 7;  // Expected total length of the buffer
		if( bytes.length<length ) {
			LOGGER.log(Level.ERROR,String.format("%s.updatePositionFromBytes: Message too short: %s",CLSS,dump(bytes)));
		}
		else if( !verifyHeader(bytes) ) {
			LOGGER.log(Level.ERROR,String.format("%s.updatePositionArrayFromBytes: Header not found: %s",CLSS,dump(bytes)));
		}
		else {
			String msg = String.format("%s.updatePositionFromBytes: Unimplemented - %s",CLSS,dump(bytes));
			LOGGER.log(Level.INFO,msg);
		}
	}
	// ===================================== Private Methods =====================================
	
	// The first 4 bytes of all messages are the same
	private static void setHeader(byte[] bytes) {
		bytes[0] = (byte)0xFF;
		bytes[1] = (byte)0xFF;
		bytes[2] = (byte)0xFD;
		bytes[3] = 0;
	}
	/**
	 *  Consider bytes 0-(len-2), then insert into last 2 bytes
	 * @param buf the byte buffer
	 */
	// From: https://introcs.cs.princeton.edu/java/61data/CRC16.java. Also see:
	// YARP: NetType.cpp and http://emanual.robotis.com/docs/en/dxl/crc
	// https://www.javatips.net/api/xDrip-master/app/src/main/java/com/eveningoutpost/dexdrip/ImportedLibraries/dexcom/CRC16.java
	public static void setCrc( byte[] buf ) {
		int size = buf.length - 2;   // Exclude bytes c]to hold CRC
		int crc = 0;
	    for( int j=0; j < size; j++ ) {
	    	byte b = buf[j];
	        crc = (crc>>>8) ^ crc_table[(crc^b) & 0xff];
	    }
	    buf[size+1] = (byte) (crc & 0xff);
	    buf[size+2] = (byte) ((crc >> 8) & 0xff);
	}

	private static boolean verifyHeader(byte[] bytes) {
		boolean result = false;
		if( bytes.length> 5 &&
			bytes[0]==0xFF  &&
			bytes[0]==0xFF  &&
			bytes[0]==0xFD  &&
			bytes[0]==0    ) {
			
			result = true;
		}
		return result;
	}
	
	
	
	/**
	 * Test using example in Robotis documentation for WRITE command and status, 5.3.3.2 and 5.3.3.3.
	 * http://emanual.robotis.com/docs/en/dxl/protocol2
	 */
	public static void main(String [] args) {
        byte[] bytes = new byte[16];
        setHeader(bytes);
        bytes[4] = 0x01; 
		bytes[5] = 0x09; 
		bytes[6] = 0;
		bytes[7] = WRITE;
		bytes[8] = 0x74; 
		bytes[9] = 0;
		bytes[10] = 0;
		bytes[11] = 0x02;
		bytes[12] = 0;
		bytes[13] = 0;
		setCrc(bytes);
		
        System.out.println("WRITE with CRC: "+dump(bytes));
        
        bytes = new byte[12];
        setHeader(bytes);
        bytes[4] = 0x01; 
		bytes[5] = 0x04; 
		bytes[6] = 0;
		bytes[7] = STATUS_RETURN;
		bytes[8] = 0; 
		bytes[9] = 0;
		setCrc(bytes);
		
        System.out.println("Status with CRC: "+dump(bytes));

    }
}
