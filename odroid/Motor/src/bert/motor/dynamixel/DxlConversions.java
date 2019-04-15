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
	private static Map<DynamixelType,Integer> resolution;         // Increments for 360 deg by type
	private static Map<DynamixelType,Double> torque;              // Max torque ~ Nm
	private static Map<DynamixelType,Double> velocity;            // Angular velocity ~ deg/s
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
		resolution = new HashMap<>();
		resolution.put(DynamixelType.AX12,1024);
		resolution.put(DynamixelType.MX28,4096); 
		resolution.put(DynamixelType.MX64,4096); 
		
		// Values ~ Nm
		torque = new HashMap<>();
		torque.put(DynamixelType.AX12,1.2);
		torque.put(DynamixelType.MX28,2.5); 
		torque.put(DynamixelType.MX64,6.0); 
	}

	
	public static double dxlToDegree(DynamixelType model,double value) {
	    int maxPosition = resolution.get(model);
	    double maxDeg   = 360.;
	    return ((maxDeg * value) / (maxPosition-1)) - maxDeg/2;
	}

	public static int degreeToDxl(DynamixelType model,double value) {
	    int maxPosition = resolution.get(model);
	    double maxDeg   = 360.;
	    int pos = (int)Math.round((maxPosition-1) * ((maxDeg/2 + value) / maxDeg));
	    pos = Math.min(Math.max(pos, 0), maxPosition - 1);
	    return pos;
	}
	// Speed is deg/sec
	public static double dxlToSpeed(DynamixelType model,double value) {
		int cw = (int)(value/1024);
		double speed = value%1024;
		int direction = (-2 * cw + 1);
		double speed_factor = 0.114;
		if( model.equals(DynamixelType.AX12) ) speed_factor = 0.111;
		return direction * (speed * speed_factor) * 6;
	}
	
	public static int speedToDxl(DynamixelType model,double value) {
		int direction = 0; 
		if( value < 0 ) direction = 1024;
		double speed_factor = 0.114;
		if( model.equals(DynamixelType.AX12) ) speed_factor = 0.111;
		double max_value = 1023 * speed_factor * 6;
		value = Math.min(Math.max(value, -max_value), max_value);
		return (int)(Math.round(direction + Math.abs(value) / (6 * speed_factor)));
	}
	
	// % of max (assumes EEPROM max torque is 1023)
	// Positive number is CW. Each unit represents .1%.
	public static int torqueToDxl(DynamixelType type,boolean isDirect,double value) {
		boolean cw = isDirect;
		if( value<0.) cw = !cw;
		int val = (int)(value*10.23/torque.get(type));
		if( !cw ) val = val | 0x400;
		return val; 
	}
	public static double dxlToTorque(DynamixelType type,boolean isDirect,byte b1,byte b2) {
		int raw = b1+256*b2;
		boolean cw = isDirect;
		if( (raw & 0x400) != 0 ) cw = !cw;
		raw = raw&0x3FF;
		double result = raw*torque.get(type);
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
		if( name.equalsIgnoreCase(JointProperty.TEMPERATURE.name())) text = String.format("%.0f degrees centigrade",value);
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
		if( name.equalsIgnoreCase(JointProperty.TEMPERATURE.name())) value = dxlToTemperature(b1);
		else if( name.equalsIgnoreCase(JointProperty.TORQUE.name())) value = dxlToTorque(type,isDirect,b1,b2);
		else if( name.equalsIgnoreCase(JointProperty.VOLTAGE.name())) value = dxlToVoltage(b1);
		else {
			LOGGER.warning(String.format("%s.dataBytesForProperty: Unrecognized property name (%s)",CLSS,name));
		}
		return value;
	}
}
