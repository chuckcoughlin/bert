/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 * Not used, as currently all our motors are configured with Protocol 1.
 */

package bert.motor.dynamixel;

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
 * to be Protocol 2.0. This class applies to preotocol 2 only.
 */
@SuppressWarnings("unused")
public class DxlMessageV2  {
	private static final String CLSS = "DxlMessageV2";
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
	
	// CRC is used for Protocol 2
	private static int[] crc_table = {
	        0x0000, 0x8005, 0x800F, 0x000A, 0x801B, 0x001E, 0x0014, 0x8011,
	        0x8033, 0x0036, 0x003C, 0x8039, 0x0028, 0x802D, 0x8027, 0x0022,
	        0x8063, 0x0066, 0x006C, 0x8069, 0x0078, 0x807D, 0x8077, 0x0072,
	        0x0050, 0x8055, 0x805F, 0x005A, 0x804B, 0x004E, 0x0044, 0x8041,
	        0x80C3, 0x00C6, 0x00CC, 0x80C9, 0x00D8, 0x80DD, 0x80D7, 0x00D2,
	        0x00F0, 0x80F5, 0x80FF, 0x00FA, 0x80EB, 0x00EE, 0x00E4, 0x80E1,
	        0x00A0, 0x80A5, 0x80AF, 0x00AA, 0x80BB, 0x00BE, 0x00B4, 0x80B1,
	        0x8093, 0x0096, 0x009C, 0x8099, 0x0088, 0x808D, 0x8087, 0x0082,
	        0x8183, 0x0186, 0x018C, 0x8189, 0x0198, 0x819D, 0x8197, 0x0192,
	        0x01B0, 0x81B5, 0x81BF, 0x01BA, 0x81AB, 0x01AE, 0x01A4, 0x81A1,
	        0x01E0, 0x81E5, 0x81EF, 0x01EA, 0x81FB, 0x01FE, 0x01F4, 0x81F1,
	        0x81D3, 0x01D6, 0x01DC, 0x81D9, 0x01C8, 0x81CD, 0x81C7, 0x01C2,
	        0x0140, 0x8145, 0x814F, 0x014A, 0x815B, 0x015E, 0x0154, 0x8151,
	        0x8173, 0x0176, 0x017C, 0x8179, 0x0168, 0x816D, 0x8167, 0x0162,
	        0x8123, 0x0126, 0x012C, 0x8129, 0x0138, 0x813D, 0x8137, 0x0132,
	        0x0110, 0x8115, 0x811F, 0x011A, 0x810B, 0x010E, 0x0104, 0x8101,
	        0x8303, 0x0306, 0x030C, 0x8309, 0x0318, 0x831D, 0x8317, 0x0312,
	        0x0330, 0x8335, 0x833F, 0x033A, 0x832B, 0x032E, 0x0324, 0x8321,
	        0x0360, 0x8365, 0x836F, 0x036A, 0x837B, 0x037E, 0x0374, 0x8371,
	        0x8353, 0x0356, 0x035C, 0x8359, 0x0348, 0x834D, 0x8347, 0x0342,
	        0x03C0, 0x83C5, 0x83CF, 0x03CA, 0x83DB, 0x03DE, 0x03D4, 0x83D1,
	        0x83F3, 0x03F6, 0x03FC, 0x83F9, 0x03E8, 0x83ED, 0x83E7, 0x03E2,
	        0x83A3, 0x03A6, 0x03AC, 0x83A9, 0x03B8, 0x83BD, 0x83B7, 0x03B2,
	        0x0390, 0x8395, 0x839F, 0x039A, 0x838B, 0x038E, 0x0384, 0x8381,
	        0x0280, 0x8285, 0x828F, 0x028A, 0x829B, 0x029E, 0x0294, 0x8291,
	        0x82B3, 0x02B6, 0x02BC, 0x82B9, 0x02A8, 0x82AD, 0x82A7, 0x02A2,
	        0x82E3, 0x02E6, 0x02EC, 0x82E9, 0x02F8, 0x82FD, 0x82F7, 0x02F2,
	        0x02D0, 0x82D5, 0x82DF, 0x02DA, 0x82CB, 0x02CE, 0x02C4, 0x82C1,
	        0x8243, 0x0246, 0x024C, 0x8249, 0x0258, 0x825D, 0x8257, 0x0252,
	        0x0270, 0x8275, 0x827F, 0x027A, 0x826B, 0x026E, 0x0264, 0x8261,
	        0x0220, 0x8225, 0x822F, 0x022A, 0x823B, 0x023E, 0x0234, 0x8231,
	        0x8213, 0x0216, 0x021C, 0x8219, 0x0208, 0x820D, 0x8207, 0x0202
	};
	
	/**
	 * Create a serial message to broadcast a ping request to all motors.
	 * This is taken directly from http://emanual.robotis.com/docs/en/dxl/protocol2/
	 * example 5.1.4.2. (crc = 31,42)
	 * @return byte array for message
	 */
	public static byte[] bytesToBroadcastPing() {
		int length = 3;  // Remaining bytes past length including 2-byte crc
		byte[] bytes = new byte[length+7];  // Account for header and length
		setHeader(bytes,BROADCAST_ID);
		bytes[5] = (byte)length; 
		bytes[6] = 0;
		bytes[7] = PING;
		setCrc(bytes);
		return bytes;
	}
	/**
	 * Create a serial message to read a specified property of a motor.
	 * @param id of the motor
	 * @param propertyName the name of the desired property (must be a joint property)
	 * @return byte array with command to read the property (protocol 2)
	 */
	public static byte[] bytesToGetPropertyV2(int id,String propertyName) {
		int length = 7;  // Remaining bytes past length including 2-byte crc
		byte[] bytes = new byte[length+7];  // Account for header and length
		setHeader(bytes,id);
		bytes[5] = (byte)length; 
		bytes[6] = 0;
		bytes[7] = READ;
		bytes[8] = DxlConversionsV2.addressForPresentProperty(propertyName);
		bytes[9] = 0;
		bytes[10] = DxlConversionsV2.dataBytesForProperty(propertyName);
		bytes[11] = 0;
		setCrc(bytes);
		return bytes;
	}
	
	/**
	 * Create a broadcast message to return a specified property of all motors.
	 * @param propertyName the name of the desired property (must be a joint property)
	 * @return byte array with broadcast command to read the property (protocol 2)
	 */
	public static byte[] bytesToListProperty(String propertyName) {
		int length = 7;  // Remaining bytes past length including 2-byte crc
		byte[] bytes = new byte[length+7];  // Account for header and length
		setHeader(bytes,BROADCAST_ID);
		bytes[5] = (byte)length; 
		bytes[6] = 0;
		bytes[7] = READ;
		bytes[8] = DxlConversionsV2.addressForPresentProperty(propertyName);
		bytes[9] = 0;
		bytes[10] = DxlConversionsV2.dataBytesForProperty(propertyName);
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
	public static byte[] bytesToSetSpeed(MotorConfiguration mc,double speed) {
		int dxlSpeed = DxlConversions.speedToDxl(mc, speed);
		int length = 7;  // Remaining bytes past length including crc
		byte[] bytes = new byte[length+7];  // Account for header and length
		setHeader(bytes,mc.getId());
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
	 * @param isDirect affects the sign of the result
	 * @param goal in n/m
	 * @return byte array with command to set speed
	 */
	public static byte[] bytesToSetTorque(MotorConfiguration mc,double goal) {
		int dxlTorque = DxlConversions.torqueToDxl(mc,goal);
		int length = 7;  // Remaining bytes past length including crc
		byte[] bytes = new byte[length+7];  // Account for header and length
		setHeader(bytes,mc.getId()); 
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
	 * @param bytes status response from the controller
	 * @param props properties from the original request
	 * @TODO  calculate parameter ...
	 */
	public static void updatePositionFromBytes(byte[] bytes,Map<String,String> props) {
		String msg = "";
		if( verifyHeader(bytes) ) {
			msg = String.format("%s.updatePositionFromBytes: %s",CLSS,dump(bytes));
			LOGGER.info(msg);
		
			int id = bytes[4];
			int err= bytes[8];
			int pos= bytes[9];
			if( err==0 ) {
				props.put(BottleConstants.PROPERTY_NAME,JointProperty.POSITION.name());
					props.put(JointProperty.POSITION.name(), String.valueOf(pos));
			}
			else {
				msg = String.format("%s.updatePositionFromBytes: message returned error %d (%s)",CLSS,err,dump(bytes));
				props.put(BottleConstants.ERROR, msg);
				LOGGER.severe(msg);
			}
		}
		else {
			msg = String.format("%s.updatePositionFromBytes: Illegal message: %s",CLSS,dump(bytes));
			props.put(BottleConstants.ERROR, msg);
			LOGGER.severe(msg);
		}
	}
	
	/**
	 * Analyze the response buffer for motor positions (probably only one) and
	 * update the supplied position map accordingly. Log errors, there's not much
	 * else we can do.
	 * @param bytes status response from the controller
	 * @param positions an array of positions by id, supplied.
	 */
	public static void updatePositionArrayFromBytes(byte[] bytes,Map<Integer,Integer> positions) {
		int length = 7;  // Expected total length of the buffer
		if( bytes.length<length ) {
			LOGGER.severe(String.format("%s.updatePositionArrayFromBytes: Message too short: %s",CLSS,dump(bytes)));
		}
		else if( verifyHeader(bytes) ) {
			String msg = String.format("%s.updatePositionArrayFromBytes: Protocol2 unimplemented - %s",CLSS,dump(bytes));
			LOGGER.info(msg);
		}
		else {
			LOGGER.severe(String.format("%s.updatePositionArrayFromBytes: Header not found: %s",CLSS,dump(bytes)));
			
		}
	}
	// ===================================== Private Methods =====================================
	// Set the header up until the length field. The header includes the device ID.
	// Protocol 2. 5 bytes
	private static void setHeader(byte[] bytes, int id) {
		bytes[0] = (byte)0xFF;
		bytes[1] = (byte)0xFF;
		bytes[2] = (byte)0xFD;
		bytes[3] = 0;
		bytes[4] = (byte)id;
	}
	
	/**
	 * Consider bytes 0-(len-2), then insert into last 2 bytes. "oversize" variables
	 * to avoid problem with no "unsigned" in Java.
	 * @see http://emanual.robotis.com/docs/en/dxl/crc
	 * @param buf the byte buffer
	 */
	public static void setCrc( byte[] buf ) {
		int size = buf.length - 2;   // Exclude bytes to hold CRC
		int crc = 0;
	    for( int j=0; j < size; j++ ) {
	    	int singleByte = buf[j];
	    	int i = ((crc>>8) ^ singleByte) & 0xFF;
	        crc = ((crc<<8) ^ crc_table[i]) & 0xFFFF;
	    }
	    buf[size]   = (byte) (crc & 0xFF);
	    buf[size+1] = (byte) ((crc >> 8) & 0xFF);
	}

	
	private static boolean verifyHeader(byte[] bytes) {
		boolean result = false;
		if( bytes.length> 7 &&
			bytes[0]==0xFF  &&
			bytes[1]==0xFF  &&
			bytes[2]==0xFD  &&
			bytes[3]==0    ) {
			
			result = true;
		}
		return result;
	}
	

	
	/**
	 * Test using example in Robotis documentation for WRITE command and status, 5.3.3.2 and 5.3.3.3.
	 * http://emanual.robotis.com/docs/en/dxl/protocol2
	 */
	public static void main(String [] args) {
		
		// Protocol 2
        byte[] bytes = bytesToBroadcastPing();
        // CRC should be CC
        System.out.println("PING with CRC: "+dump(bytes));
        
        bytes = new byte[16];
        setHeader(bytes,0x01); 
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
		// Should be CA, 89
        System.out.println("WRITE  with CRC: "+dump(bytes));
        
        bytes = new byte[11];
        setHeader(bytes,0x01); 
		bytes[5] = 0x04; 
		bytes[6] = 0;
		bytes[7] = STATUS_RETURN;
		bytes[8] = 0; 
		setCrc(bytes);
		// Should be A1 0C
        System.out.println("Status with CRC: "+dump(bytes));

    }
}