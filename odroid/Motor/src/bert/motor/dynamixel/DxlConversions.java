/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.motor.dynamixel;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import bert.share.common.DynamixelType;
import bert.share.motor.JointProperty;

/**
 * This class contains static methods used to convert between Dynamixel jointValues and engineering units.
 * Code is derived from Pypot dynamixel.conversion.py
 * Protocol 1 only.
 */
public class DxlConversions  {
	private static final String CLSS = "DynamixelConversions";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static Map<DynamixelType,Integer> range;         // Range of motion in degrees
	private static Map<DynamixelType,Integer> resolution;    // Increments for 360 deg by type
	private static Map<DynamixelType,Double> torque;         // Max torque ~ Nm
	private static Map<DynamixelType,Double> velocity;       // Angular velocity ~ deg/s
	// Constants for control table addressses. These must be the same for MX28,MX64,AX12
	private static final byte GOAL_POSITION    = (byte)0x1E;  // low, high bytes
	private static final byte MOVING_SPEED     = (byte)0x20;  // low, high bytes
	private static final byte TORQUE_LIMIT     = (byte)0x22;  // low, high bytes
	private static final byte PRESENT_LOAD     = (byte)0x28;  // low, high bytes
	private static final byte PRESENT_POSITION = (byte)0x24;  // low, high bytes
	private static final byte PRESENT_SPEED    = (byte)0x26;  // low, high bytes
	private static final byte PRESENT_TEMPERATURE = (byte)0x2B;  // single byte
	private static final byte PRESENT_VOLTAGE     = (byte)0x2A;  // single byte
	
	static {
		range = new HashMap<>();
		range.put(DynamixelType.AX12,300);
		range.put(DynamixelType.MX28,360); 
		range.put(DynamixelType.MX64,360); 
		
		resolution = new HashMap<>();
		resolution.put(DynamixelType.AX12,1024);
		resolution.put(DynamixelType.MX28,4096); 
		resolution.put(DynamixelType.MX64,4096); 
		
		// Full-scale speeds ~ deg/sec
		velocity = new HashMap<>();
		velocity.put(DynamixelType.AX12,684.);
		velocity.put(DynamixelType.MX28,700.); 
		velocity.put(DynamixelType.MX64,700.);
			    
		// Full-scale torque ~ Nm
		torque = new HashMap<>();
		torque.put(DynamixelType.AX12,1.2);
		torque.put(DynamixelType.MX28,2.5); 
		torque.put(DynamixelType.MX64,6.0); 
	}

	// Offset so that 0 deg is the reference.
	public static int degreeToDxl(DynamixelType type,boolean isDirect,double value) {
		int r = range.get(type);
		int offset = r/2;
		int res = resolution.get(type);
		if( isDirect ) value = -value;
		int val = (int)(value+offset)*res/r;
		return val;
	}
	public static double dxlToDegree(DynamixelType type,boolean isDirect,byte b1,byte b2) {
		int raw = b1+256*b2;
		int r = range.get(type);
		int offset = r/2;
		int res = resolution.get(type);
		double result = raw*r/res - offset;
		if( isDirect ) result = -result;
		return result;
	}
	// Speed is deg/sec
	public static int speedToDxl(DynamixelType type,boolean isDirect,double value) {
		boolean cw = isDirect;
		if( value<0.) cw = !cw;
		int val = (int)(value*1023./velocity.get(type));
		if( !cw ) val = val | 0x400;
		return val;
	}
	public static double dxlToSpeed(DynamixelType type,boolean isDirect,byte b1,byte b2) {
		int raw = b1+256*b2;
		boolean cw = isDirect;
		if( (raw & 0x400) != 0 ) cw = !cw;
		raw = raw&0x3FF;
		double result = raw*velocity.get(type)/1023;
		if(!cw) result = -result;
		return result;
	}
	
	// N-m (assumes EEPROM max torque is 1023)
	// Positive number is CW. Each unit represents .1%.
	public static int torqueToDxl(DynamixelType type,boolean isDirect,double value) {
		boolean cw = isDirect;
		if( value<0.) cw = !cw;
		int val = (int)(value*1023./torque.get(type));
		if( !cw ) val = val | 0x400;
		return val; 
	}
	public static double dxlToTorque(DynamixelType type,boolean isDirect,byte b1,byte b2) {
		int raw = b1+256*b2;
		boolean cw = isDirect;
		if( (raw & 0x400) != 0 ) cw = !cw;
		raw = raw&0x3FF;
		double result = raw*torque.get(type)/1023.;
		if(!cw) result = -result;
		return result;
	}
	// deg C
	public static double dxlToTemperature(byte value) { return (double)value; }
	// volts
	public static double dxlToVoltage(byte value) { return ((double)value)/10.; }

	// Convert the named property to a control table address for the present state
	// of that property. These need to  be independent of motor type.
	public static byte addressForPresentProperty(String name) {
		byte address = 0;
		if( name.equalsIgnoreCase(JointProperty.POSITION.name())) address = PRESENT_POSITION;
		else if( name.equalsIgnoreCase(JointProperty.SPEED.name())) address = PRESENT_SPEED;
		else if( name.equalsIgnoreCase(JointProperty.TEMPERATURE.name())) address = PRESENT_TEMPERATURE;
		else if( name.equalsIgnoreCase(JointProperty.TORQUE.name())) address = PRESENT_LOAD;
		else if( name.equalsIgnoreCase(JointProperty.VOLTAGE.name())) address = PRESENT_VOLTAGE;
		else {
			LOGGER.warning(String.format("%s.addressForProperty: Unrecognized property name (%s)",CLSS,name));
		}
		return address;
	}
	// Return the data length for the named property in the control table. We assume that
	// present values and goals are the same number of bytes.
	// Valid for Protocol 1 only.
	public static byte dataBytesForProperty(String name) {
		byte length = 0;
		if( name.equalsIgnoreCase(JointProperty.POSITION.name())) length = 2;
		else if( name.equalsIgnoreCase(JointProperty.SPEED.name())) length = 2;
		else if( name.equalsIgnoreCase(JointProperty.TEMPERATURE.name())) length = 1;
		else if( name.equalsIgnoreCase(JointProperty.TORQUE.name())) length = 2;
		else if( name.equalsIgnoreCase(JointProperty.VOLTAGE.name())) length = 1;
		else {
			LOGGER.warning(String.format("%s.dataBytesForProperty: Unrecognized property name (%s)",CLSS,name));
		}
		return length;
	}
	// Convert the raw data bytes text describing the value and units. It may or may not use the second byte.
	// Presumably the ultimate will have more context.
	public static String textForProperty(String name,DynamixelType type,boolean isDirect,byte b1,byte b2) {
		name = name.toLowerCase();
		String text = "";
		double value = valueForProperty(name,type,isDirect,b1,b2);
		if( name.equalsIgnoreCase(JointProperty.POSITION.name())) text = String.format("%.0f degrees clock-wise",value);
		else if( name.equalsIgnoreCase(JointProperty.SPEED.name())) text = String.format("%.0f degrees per second",value);
		else if( name.equalsIgnoreCase(JointProperty.TEMPERATURE.name())) text = String.format("%.0f degrees centigrade",value);
		else if( name.equalsIgnoreCase(JointProperty.TORQUE.name())) text = String.format("%.0f newton-meters",value);
		else if( name.equalsIgnoreCase(JointProperty.VOLTAGE.name()))text = String.format("%.1f volts",value);
		else {
			LOGGER.warning(String.format("%s.textForProperty: Unrecognized property name (%s)",CLSS,name));
		}
		return text;
	}
	// Convert the raw data bytes into a double value. It may or may not use the second byte.
	// Valid for Protocol 1 only.
	public static double valueForProperty(String name,DynamixelType type,boolean isDirect,byte b1,byte b2) {
		double value = 0.;
		if( name.equalsIgnoreCase(JointProperty.POSITION.name())) value = dxlToDegree(type,isDirect,b1,b2);
		else if( name.equalsIgnoreCase(JointProperty.SPEED.name())) value = dxlToSpeed(type,isDirect,b1,b2);
		else if( name.equalsIgnoreCase(JointProperty.TEMPERATURE.name())) value = dxlToTemperature(b1);
		else if( name.equalsIgnoreCase(JointProperty.TORQUE.name())) value = dxlToTorque(type,isDirect,b1,b2);
		else if( name.equalsIgnoreCase(JointProperty.VOLTAGE.name())) value = dxlToVoltage(b1);
		else {
			LOGGER.warning(String.format("%s.dataBytesForProperty: Unrecognized property name (%s)",CLSS,name));
		}
		return value;
	}
}
