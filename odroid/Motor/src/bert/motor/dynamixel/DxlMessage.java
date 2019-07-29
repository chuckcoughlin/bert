/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.motor.dynamixel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import bert.share.common.DynamixelType;
import bert.share.message.BottleConstants;
import bert.share.motor.Joint;
import bert.share.motor.JointProperty;
import bert.share.motor.MotorConfiguration;
import bert.sql.db.Database;

/**
 * This class contains utility methods used to create and interpret different varieties \
 * of Dynamixel serial messages. Code is derived from Pypot dynamixel.v2.py and the Dynamixel
 * documentation at http://emanual.robotis.com. Applies to MX64, MX28, AX12A models.
 * The documentation is unclear about the protocol version for AX-12 models, but it appears
 * to be Protocol 2.0. We have coded on this assumption.
 */
@SuppressWarnings("unused")
public class DxlMessage  {
	private static final String CLSS = "DxlMessage";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	// Constants for the instructions
	private static final byte BROADCAST_ID = (byte)0xFE; // ID to transmit to all devices connected to port
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
	private final DxlConversions converter;
	private long travelTime = 0;
	
	public DxlMessage() {
		this.converter = new DxlConversions();
	}
	/**
	 * As each method that generates motor motions is invoked, it calculates time to execute the movement.
	 * The result is stored as a static parameter, good only until the next method is run.
	 * @return the maximum travel time as calculated by the most recent byte syntax generator. Time ~msecs. 
	 */
	public long getMostRecentTravelTime() { return travelTime; }
	
	/**
	 * Iterate through the list of motor configurations to determine which, if any, are outside the max-min
	 * angle ranges. For those outside, move the position to a legal value.
	 * WARNING: SYNC_WRITE requests, apparently, do not generate responses.
	 * Discount any current readings of zero, it probably means that the motor positions were never evaluated.
	 * @param configurations a list of motor configuration objects
	 * @return list of byte arrays with bulk read plus extras for any AX-12. 
	 */
	public List<byte[]> byteArrayListToInitializePositions(Collection<MotorConfiguration> configurations) {
		List<MotorConfiguration> outliers = new ArrayList<>();  // Will hold the joints that need moving.
		
		travelTime = 0;
		for(MotorConfiguration mc:configurations) {
			double pos = mc.getPosition();
			if( pos==0. ) {
				LOGGER.info(String.format("%s.byteArrayListToInitializePositions: %s never evaluated, ignored",CLSS,mc.getJoint().name()));
			}
			else if( pos>mc.getMaxAngle() ) {
				LOGGER.info(String.format("%s.byteArrayListToInitializePositions: %s out-of-range at %.0f (max=%.0f)",
											CLSS,mc.getJoint().name(),pos,mc.getMaxAngle()));
				mc.setPosition(mc.getMaxAngle());
				outliers.add(mc);
				if(mc.getTravelTime()>travelTime) travelTime = mc.getTravelTime();
			}
			else if(pos<mc.getMinAngle()) {
				LOGGER.info(String.format("%s.byteArrayListToInitializePositions: %s out-of-range at %.0f (min=%.0f)",
											CLSS,mc.getJoint().name(),pos,mc.getMinAngle()));
				mc.setPosition(mc.getMinAngle());
				outliers.add(mc);
				if(mc.getTravelTime()>travelTime) travelTime = mc.getTravelTime();
			}
		}
		
		List<byte[]> messages = new ArrayList<>();
		int pc = outliers.size();
		// Positions
		if( pc>0 ) {
			int len = (3 * pc) + 8;  //  3 bytes per motor + address + byte count + header + checksum
			byte[] bytes = new byte[len];
			setSyncWriteHeader(bytes);
			bytes[3] = (byte)(len-4);
			bytes[4] = SYNC_WRITE;
			bytes[5] = converter.addressForGoalProperty(JointProperty.POSITION.name());
			bytes[6] = 0x2;  // 2 bytes
			int index = 7;
			for( MotorConfiguration mc:outliers) {
				LOGGER.info(String.format("%s.byteArrayListToInitializePositions: set position for %s to %.0f",CLSS,mc.getJoint().name(),mc.getPosition()));
				int dxlValue = converter.dxlValueForProperty(JointProperty.POSITION.name(),mc,mc.getPosition());
				bytes[index]= (byte) mc.getId();
				bytes[index+1] = (byte)(dxlValue & 0xFF);
				bytes[index+2] = (byte)(dxlValue >>8);
				index = index+3;
			}
			setChecksum(bytes);
			messages.add(bytes);
		}
		return messages;
	}
	/**
	 * Create a bulk read message to interrogate a list of motors for a specified
	 * property. Unfortunately AX-12 motors do not support this request, so must be
	 * queried separately (thus the list). Note that the bulk read results in individual
	 * responses from each motor.
	 * @param propertyName the name of the desired property (must be a joint property)
	 * @return list of byte arrays with bulk read plus extras for any AX-12. 
	 */
	public List<byte[]> byteArrayListToListProperty(String propertyName,Collection<MotorConfiguration> configurations) {
		List<byte[]> messages = new ArrayList<>();
		int count = configurations.size();   // Number of motors, less AX-12
		for( MotorConfiguration mc:configurations) {
			if(mc.getType().equals(DynamixelType.AX12) ) {
				int length = 4;  // Remaining bytes past length including checksum
				byte[] bytes = new byte[length+4];  // Account for header and length
				setHeader(bytes,mc.getId());
				bytes[3] = (byte)length; 
				bytes[4] = READ;
				bytes[5] = converter.addressForPresentProperty(propertyName);
				bytes[6] = converter.dataBytesForProperty(propertyName);
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
			bytes[addr] = converter.dataBytesForProperty(propertyName);
			bytes[addr+1] = (byte)mc.getId();
			bytes[addr+2] = converter.addressForPresentProperty(propertyName);
			addr += 3;
		}
		setChecksum(bytes);
		messages.add(bytes);
		return messages;
	}
	/**
	 * Set either the speeds or torque for motors in the specified limb. The motor configurations have all the
	 * information.
	 * WARNING: SYNC_WRITE requests, apparently, do not generate responses.
	 * @param map of the motor configurations keyed by joint name
	 * @param limb name of the limb to be set
	 * @param property, either speed or torque
	 * @return a byte array with entries corresponding to joints of the limb, if any. 
	 */
	public byte[] byteArrayToSetLimbProperty(Map<String,MotorConfiguration> map,String limb,String property) {
		boolean isTorque = true;
		if( property.equalsIgnoreCase("speed")) isTorque = false;
		
		// First count all the joints in the limb
		int count = 0;
		for( MotorConfiguration mc:map.values()) {
			if( mc.getLimb().name().equalsIgnoreCase(limb)) count++;
		}
		
		byte[] bytes = new byte[0];
		int dxlValue = 0;
		if( count>0 ) {
			int len = (3 * count) + 8;  //  3 bytes per motor + address + byte count + header + checksum
			bytes = new byte[len];
			setSyncWriteHeader(bytes);
			bytes[3] = (byte)(len-4);
			bytes[4] = SYNC_WRITE;
			bytes[5] = converter.addressForGoalProperty(JointProperty.TORQUE.name());
			bytes[6] = 0x2;  // 2 bytes
			int index = 7;
			for( MotorConfiguration mc:map.values()) {
				if( mc.getLimb().name().equalsIgnoreCase(limb)) {
					if( isTorque ) {
						dxlValue = converter.dxlValueForProperty(JointProperty.TORQUE.name(),mc,mc.getTorque());
					}
					else {
						dxlValue = converter.dxlValueForProperty(JointProperty.SPEED.name(),mc,mc.getSpeed());
					}
					bytes[index]= (byte) mc.getId();
					bytes[index+1] = (byte)(dxlValue & 0xFF);
					bytes[index+2] = (byte)(dxlValue >>8);
					index = index+3;
				}
			}
			setChecksum(bytes);
		}

		return bytes;
	}
	/**
	 * A pose may consist of any or all of position, speed and torque for the motors it refrerences. Query the database
	 * to get values. Skip any that have null values. There is a hardware limit of 143 bytes for each array (shouldn't be a problem).
	 * WARNING: SYNC_WRITE requests, apparently, do not generate responses.
	 * @param map of the motor configurations keyed by joint name
	 * @param pose name of the pose to be set
	 * @return up to 3 byte arrays as required by the pose
	 */
	public List<byte[]> byteArrayListToSetPose(Map<String,MotorConfiguration> map,String pose) {
		Database db = Database.getInstance();
		Map<String,Double>torques = db.getPoseJointValuesForParameter(map,pose,"torque");
		Map<String,Double>speeds = db.getPoseJointValuesForParameter(map,pose,"speed");
		Map<String,Double>positions = db.getPoseJointValuesForParameter(map,pose,"position");
		
		List<byte[]> messages = new ArrayList<>();
		// First set torques, then speeds, then positions
		int tc = torques.size();
		// Torque
		if( tc>0 ) {
			int len = (3 * tc) + 8;  //  3 bytes per motor + address + byte count + header + checksum
			byte[] bytes = new byte[len];
			setSyncWriteHeader(bytes);
			bytes[3] = (byte)(len-4);
			bytes[4] = SYNC_WRITE;
			bytes[5] = converter.addressForGoalProperty(JointProperty.TORQUE.name());
			bytes[6] = 0x2;  // 2 bytes
			int index = 7;
			for( String key:torques.keySet()) {
				MotorConfiguration mc = map.get(key);
				int dxlValue = converter.dxlValueForProperty(JointProperty.TORQUE.name(),mc,torques.get(key));
				bytes[index]= (byte) mc.getId();
				bytes[index+1] = (byte)(dxlValue & 0xFF);
				bytes[index+2] = (byte)(dxlValue >>8);
				mc.setTorque(torques.get(key));   // percent of max
				index = index+3;
			}
			setChecksum(bytes);
			messages.add(bytes);
		}
		
		int sc = speeds.size();
		// Speed
		if( sc>0 ) {
			int len = (3 * sc) + 8;  //  3 bytes per motor + address + byte count + header + checksum
			byte[] bytes = new byte[len];
			setSyncWriteHeader(bytes);
			bytes[3] = (byte)(len-4);
			bytes[4] = SYNC_WRITE;
			bytes[5] = converter.addressForGoalProperty(JointProperty.SPEED.name());
			bytes[6] = 0x2;  // 2 bytes
			int index = 7;
			for( String key:speeds.keySet()) {
				MotorConfiguration mc = map.get(key);
				int dxlValue = converter.dxlValueForProperty(JointProperty.SPEED.name(),mc,speeds.get(key));
				bytes[index]= (byte) mc.getId();
				bytes[index+1] = (byte)(dxlValue & 0xFF);
				bytes[index+2] = (byte)(dxlValue >>8);
				mc.setSpeed(speeds.get(key));   // percent of max
				index = index+3;
			}
			setChecksum(bytes);
			messages.add(bytes);
		}
		int pc = positions.size();
		// Positions
		if( pc>0 ) {
			travelTime = 0;
			int len = (3 * pc) + 8;  //  3 bytes per motor + address + byte count + header + checksum
			byte[] bytes = new byte[len];
			setSyncWriteHeader(bytes);
			bytes[3] = (byte)(len-4);
			bytes[4] = SYNC_WRITE;
			bytes[5] = converter.addressForGoalProperty(JointProperty.POSITION.name());
			bytes[6] = 0x2;  // 2 bytes
			int index = 7;
			for( String key:positions.keySet()) {
				MotorConfiguration mc = map.get(key);
				//LOGGER.info(String.format("%s.bytesToSetPose: Id = %d - set position for %s to %.0f",CLSS,mc.getId(),key,positions.get(key)));
				int dxlValue = converter.dxlValueForProperty(JointProperty.POSITION.name(),mc,positions.get(key));
				bytes[index]= (byte) mc.getId();
				bytes[index+1] = (byte)(dxlValue & 0xFF);
				bytes[index+2] = (byte)(dxlValue >>8);
				mc.setPosition(positions.get(key));
				if(mc.getTravelTime()>travelTime) travelTime = mc.getTravelTime();
				index = index+3;
			}
			setChecksum(bytes);
			messages.add(bytes);
		}
		return messages;
	}
	/**
	 * Create a serial message to broadcast a ping request to all motors.
	 * This is taken directly from http://emanual.robotis.com/docs/en/dxl/protocol1/
	 * @return byte array for message
	 */
	public byte[] bytesToBroadcastPing() {
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
	public byte[] bytesToGetGoals(int id) {
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
	public byte[] bytesToGetLimits(int id) {
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
	public byte[] bytesToGetProperty(int id,String propertyName) {
		int length = 4;  // Remaining bytes past length including checksum
		byte[] bytes = new byte[length+4];  // Account for header and length
		setHeader(bytes,id);
		bytes[3] = (byte)length; 
		bytes[4] = READ;
		bytes[5] = converter.addressForPresentProperty(propertyName);
		bytes[6] = converter.dataBytesForProperty(propertyName);
		setChecksum(bytes);
		return bytes;
	}

	/**
	 * Create a serial message to write a goal for the motor. Recognized properties are:
	 * position, speed and torque. All are two byte parameters.
	 * @param id of the motor
	 * @param propertyName the name of the desired property (must be a joint property)
	 * @return byte array with command to read the property
	 */
	public byte[] bytesToSetProperty(MotorConfiguration mc,String propertyName,double value) {
		int dxlValue = converter.dxlValueForProperty(propertyName,mc,value);
		int length = 5;  // Remaining bytes past length including checksum
		byte[] bytes = new byte[length+4];  // Account for header and length
		setHeader(bytes,mc.getId());
		bytes[3] = (byte)length; 
		bytes[4] = WRITE;
		bytes[5] = converter.addressForGoalProperty(propertyName);
		bytes[6] = (byte)(dxlValue & 0xFF);
		bytes[7] = (byte)(dxlValue >>8);
		setChecksum(bytes);
		if( propertyName.equalsIgnoreCase(JointProperty.POSITION.name())) {
			mc.setPosition(value);
			travelTime = mc.getTravelTime();
		}
		else if( propertyName.equalsIgnoreCase(JointProperty.SPEED.name())) mc.setSpeed(value);
		else if( propertyName.equalsIgnoreCase(JointProperty.TORQUE.name()))mc.setTorque(value);
		return bytes;
	}
	/**
	 * Create a string suitable for printing and debugging.
	 * @param bytes
	 * @return a formatted string of the bytes as hex digits.
	 */
	public String dump(byte[] bytes) {
		StringBuffer sb = new StringBuffer();
		int index = 0;
		if( bytes!=null ) {
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
		}
		else {
			sb.append("null message");
		}
		return sb.toString();
	}
	/**
	 * Scan the supplied byte array looking for the message start markers.
	 * When found return the buffer less any leading junk.
	 * @param bytes
	 * @return buffer guaranteed to be a legal message start, else null.
	 */
	public byte[] ensureLegalStart(byte[] bytes) {
		int i = 0;
		while(i<bytes.length-2) {
			if( bytes[i]==(byte)0xFF  &&
				bytes[i]==(byte)0xFF	) {
				if( i==0 ) return bytes;
				else {
					byte[] copy = new byte[bytes.length-i];
					System.arraycopy(bytes, i, copy, 0, copy.length);
					LOGGER.warning(String.format("%s.ensureLegalStart: cut %d bytes to provide legal msg",CLSS,i));
					return copy;
				}
			}
			i++;
		}
		return null;
	}
	/**
	 * The only interesting information in a status message from a write 
	 * to a single device is the error code.
	 * @param bytes
	 * @return
	 */
	public String errorMessageFromStatus(byte[] bytes) {
		String msg = null;
		if( bytes.length >4 ) {
			byte error = bytes[4];
			if( error!=0x00 ) {
				byte id = bytes[2];
				msg = String.format("Motor %d encountered %s",id,descriptionForError(error));
				LOGGER.severe(msg);
			}
		}
		return msg;
	}
	/**
	 * Extract the message length. 
	 * @param bytes
	 * @return the total number of bytes in message, else -1 if there are too few bytes to tell.
	 */
	public int getMessageLength(byte[] bytes) {
		int len = -1;
		if( bytes.length>-4 ) {
			len = bytes[3]+4;
		}
		return len;
	}
	/**
	 * Analyze a response buffer returned from a request for goal values for a motor. Goals
	 * parameters are: position, speed, torque. Results will be entered in the properties map.
	 * Convert speeds and torques to percent of max disregarding direction.
	 * @param type the model of the motor
	 * @param isDirect the orientation of the motor
	 * @param props properties from a MessageBottle
	 * @param bytes status response from the controller
	 */
	public void updateGoalsFromBytes(MotorConfiguration mc,Map<String,String> props,byte[] bytes) {
		String msg = "";
		if( verifyHeader(bytes) ) {
			msg = String.format("%s.updateGoalsFromBytes: %s",CLSS,dump(bytes));
		
			int id = bytes[2];
			byte err= bytes[4];
			
			String parameterName = JointProperty.POSITION.name();
			double v1 = converter.valueForProperty(parameterName,mc,bytes[5],bytes[6]);
			String t1  = converter.textForProperty(parameterName,mc,bytes[5],bytes[6]);
			props.put(parameterName,String.valueOf(v1));
			mc.setPosition(v1);
			
			parameterName = JointProperty.SPEED.name();    // Non-directional
			double v2 = converter.valueForProperty(parameterName,mc,bytes[7],bytes[8]);
			String t2  = converter.textForProperty(parameterName,mc,bytes[7],bytes[8]);
			props.put(parameterName,String.valueOf(v2));
			v2 = v2*100./DxlConversions.velocity.get(mc.getType());// Convert to percent
			mc.setSpeed(v2);  
			
			parameterName = JointProperty.TORQUE.name();   // Non-directional
			double v3 = converter.valueForProperty(parameterName,mc,bytes[9],bytes[10]);
			String t3  = converter.textForProperty(parameterName,mc,bytes[9],bytes[10]);
			props.put(parameterName,String.valueOf(v3));
			v3 = v2*100./DxlConversions.torque.get(mc.getType());// Convert to percent
			mc.setTorque(v3);
			
			String text = String.format("Goal position, speed and torque are : %s, %s, %s", t1,t2,t3);
			if( err==0 ) {
				props.put(BottleConstants.TEXT,text);	
			}
			else {
				msg = String.format("%s.updateGoalsFromBytes: message returned error %d (%s)",CLSS,err,descriptionForError(err));
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
	 * angles and torque. These are NOT corrected for offset or orientation.
	 * @param type the model of the motor
	 * @param isDirect the orientation of the motor
	 * @param props properties from a MessageBottle
	 * @param bytes status response from the controller
	 */
	public void updateLimitsFromBytes(MotorConfiguration mc,Map<String,String> props,byte[] bytes) {
		String msg = "";
		mc.setIsDirect(true);   
		mc.setOffset(0.0);
		if( verifyHeader(bytes) ) {
			msg = String.format("%s.updateLimitsFromBytes: %s",CLSS,dump(bytes));
		
			int id = bytes[2];
			byte err= bytes[4];
			
			String parameterName = JointProperty.MAXIMUMANGLE.name(); // CW
			double v1 = converter.valueForProperty(parameterName,mc,bytes[5],bytes[6]);
			String t1  = converter.textForProperty(parameterName,mc,bytes[5],bytes[6]);
			props.put(parameterName,String.valueOf(v1));
			
			parameterName = JointProperty.MINIMUMANGLE.name();   // CCW
			double v2 = converter.valueForProperty(parameterName,mc,bytes[7],bytes[8]);
			String t2  = converter.textForProperty(parameterName,mc,bytes[7],bytes[8]);
			props.put(parameterName,String.valueOf(v2));
			
			parameterName = JointProperty.TORQUE.name();    // Non-directional
			double v3 = converter.valueForProperty(parameterName,mc,bytes[12],bytes[13]);
			String t3  = converter.textForProperty(parameterName,mc,bytes[12],bytes[13]);
			props.put(parameterName,String.valueOf(v3));
			
			String text = String.format("min, max angle and torque limits are : %s, %s, %s", t2,t1,t3);
			if( err==0 ) {
				props.put(BottleConstants.TEXT,text);	
			}
			else {
				msg = String.format("%s.updateLimitsFromBytes: message returned error %d (%s)",CLSS,err,descriptionForError(err));
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
	public void updateParameterFromBytes(String parameterName,MotorConfiguration mc,Map<String,String> props,byte[] bytes) {
		String msg = "";
		if( verifyHeader(bytes) ) {
			msg = String.format("%s.updateParameterFromBytes: %s",CLSS,dump(bytes));
		
			int id = bytes[2];
			byte err= bytes[4];
			
			double value = converter.valueForProperty(parameterName,mc,bytes[5],bytes[6]);
			String text = converter.textForProperty(parameterName,mc,bytes[5],bytes[6]);
			if( err==0 ) {
				props.put(BottleConstants.PROPERTY_NAME,parameterName);
				props.put(BottleConstants.TEXT,text);
				props.put(parameterName,String.valueOf(value));
				mc.setProperty(parameterName, value);
				LOGGER.info(String.format("%s.updateParameterFromBytes: %s %s=%.0f",CLSS,mc.getJoint(),parameterName,value));
			}
			else {
				msg = String.format("%s.updateParameterFromBytes: message returned error %d (%s)",CLSS,err,descriptionForError(err));
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
	public void updateParameterArrayFromBytes(String parameterName, Map<Integer,MotorConfiguration> configurations,byte[] bytes,Map<Integer,String> parameters) {
		String msg = "";
		int length = 7;
		int index  = 0;
		while( index<bytes.length ) {
			LOGGER.info(String.format("%s.updateParameterArrayFromBytes: index %d of %d",CLSS,index,bytes.length));
			if( verifyHeader(bytes,index) ) {
				int id = bytes[index+2];
				length = bytes[index+3] + 4;  // Takes care of fixed bytes pre-length
				byte err= bytes[index+4];
				MotorConfiguration mc =  configurations.get(id);
				if( err==0 && mc!=null && bytes.length>index+6 ) {
					double param= converter.valueForProperty(parameterName,mc,bytes[index+5],bytes[index+6]);
					parameters.put(id, String.valueOf(param));
					mc.setProperty(parameterName, param);
					LOGGER.info(String.format("%s.updateParameterArrayFromBytes: %s %s=%.0f",CLSS,mc.getJoint(),parameterName,param));
				}
				else if(err!=0){
					msg = String.format("%s.updateParameterArrayFromBytes: motor %d returned error %d (%s)",CLSS,id,err,
							descriptionForError(err));
					LOGGER.severe(msg);
				}
				// mc = null
				else if( mc==null ) {
					msg = String.format("%s.updateParameterArrayFromBytes: motor %d not supplied in motor configurations",CLSS,id,dump(bytes));
					LOGGER.severe(msg);
				}
				// NOTE: Error was not repeatable. Did read not include entire message?
				else if(bytes.length<=index+6){
					msg = String.format("%s.updateParameterArrayFromBytes: motor %d input truncated (%s)",CLSS,id,dump(bytes));
					LOGGER.severe(msg);
				}
				// Don't know what this could be ...
				else {
					msg = String.format("%s.updateParameterArrayFromBytes: programming error at id=%d",CLSS,id,dump(bytes));
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
	// Return a string describing the error. We only check one bit.
	private String descriptionForError(byte err) {
		String description = "Unrecognized error";
		if( (err&0x01) != 0x00 ) description = "an instruction error";
		else if( (err&0x02) != 0x00 ) description = "an overload error";
		else if( (err&0x04) != 0x00 ) description = "an incorrect checksum";
		else if( (err&0x08) != 0x00 ) description = "a range error";
		else if( (err&0x10) != 0x00 ) description = "overheating";
		else if( (err&0x20) != 0x00 ) description = "a position outside angle limits";
		else if( (err&0x40) != 0x00 ) description = "an input voltage outside the acceptable range";
		return description;
	}
	// Set the header up until the length field. The header includes the device ID.
	// Protocol 1. 3 bytes
	private void setHeader(byte[] bytes, int id) {
		bytes[0] = (byte)0xFF;
		bytes[1] = (byte)0xFF;
		bytes[2] = (byte) id;
	}
	// Set the header up until the length field. The header includes the device ID.
	// Protocol 1. 3 bytes
	private void setSyncWriteHeader(byte[] bytes) {
		bytes[0] = (byte)0xFF;
		bytes[1] = (byte)0xFF;
		bytes[2] = (byte)0xFE;
	}
	
	/**
	 * Consider bytes 0-(len-2), then insert into last bytes. "oversize" variables
	 * to avoid problem with no "unsigned" in Java. Ultimately we discard all except
	 * low order bits.
	 * @see http://emanual.robotis.com/docs/en/dxl/protocol1/
	 * @param buf the byte buffer
	 */
	public void setChecksum( byte[] buf ) {
		int size = buf.length - 1;   // Exclude bytes that hold Checksum
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
	private boolean verifyHeader(byte[] bytes) {
		boolean result = false;
		if( bytes.length > 5     &&
			bytes[0]==(byte)0xFF &&
			bytes[1]==(byte)0xFF  
		  ) {
			
			result = true;
		}
		return result;
	}

	private boolean verifyHeader(byte[] bytes,int index) {
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
		DxlMessage dxl = new DxlMessage();
		byte[] bytes = new byte[7];
		dxl.setHeader(bytes,0x01);
		bytes[3] = 4;    // Bytes past this field.
		bytes[4] = READ;
		bytes[5] = 0x2B;
		bytes[6] = 0x1;
		dxl.setChecksum(bytes);
		// Should be CC
        System.out.println("READ  with checksum: "+dxl.dump(bytes));
        
        // Protocol 1
        bytes = dxl.bytesToBroadcastPing();
        // Checksum should be FE
        System.out.println("PING (1)  with checksum: "+dxl.dump(bytes));
        
        // Protocol 1
        // Sync write
        bytes = new byte[18];
        bytes[0] = (byte)0xFF;
        bytes[1] = (byte)0xFF;
        bytes[2] = (byte)0xFE;
		bytes[3] = (byte)0x0E;
		bytes[4] = SYNC_WRITE;
		bytes[5] = (byte)0x1E;
		bytes[6] = (byte)0x04;
		bytes[7] = (byte)0x00;
		bytes[8] = (byte)0x10;
		bytes[9] = (byte)0x00;
		bytes[10] = (byte)0x50;
		bytes[11] = (byte)0x01;
		bytes[12] = (byte)0x01;
		bytes[13] = (byte)0x20;
		bytes[14] = (byte)0x02;
		bytes[15] = (byte)0x60;
		bytes[16] = (byte)0x03;
		dxl.setChecksum(bytes);
        // Checksum should be 67
        System.out.println("SYNC WRITE  with checksum: "+dxl.dump(bytes));

    }
}
