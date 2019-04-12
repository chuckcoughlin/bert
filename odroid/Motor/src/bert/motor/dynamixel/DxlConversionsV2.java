/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 * Not used, as currently all our motors are configured with Protocol 1.
 */

package bert.motor.dynamixel;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import bert.share.common.DynamixelType;
import bert.share.motor.JointProperty;

/**
 * This class contains static methods used to convert between Dynamixel jointValues and engineering units.
 * Code is derived from Pypot dynamixel.conversion.py and ROBOTIS documentation
 * e.g.: http://support.robotis.com/en/product/actuator/dynamixel/mx_series/mx-28(2.0).htm
 * Protocol version 2.
 */
public class DxlConversionsV2  {
	private static final String CLSS = "DynamixelConversionsV2";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static Map<DynamixelType,Integer> resolution;         // Increments for 360 deg by type
	private static Map<DynamixelType,Double> torque;              // Max torque ~ Nm
	private static Map<DynamixelType,Double> velocity;            // Angular velocity ~ deg/s

	// Constants for control table addressses. These must be the same for MX28,MX64,AX12
	private static final byte GOAL_POSITION    = (byte)116; 
	private static final byte GOAL_VELOCITY    = (byte)104;
	private static final byte PRESENT_POSITION = (byte)132; 
	private static final byte PRESENT_SPEED    = (byte)128; 
	private static final byte PRESENT_LOAD     = (byte)126; 
	
	static {
		resolution = new HashMap<>();
		resolution.put(DynamixelType.AX12,1024);
		resolution.put(DynamixelType.MX28,4096); 
		resolution.put(DynamixelType.MX64,4096); 
		
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
	
	public static double dxlToTorque(DynamixelType model,double value) {
	    return value / 10.23;
	}

	public static int torqueToDxl(DynamixelType model,double value) {
	    return (int)Math.round(value * 10.23);
	}
	
	// Convert the named property to a control table address for the present state
	// of that property. These need to  be independent of motor type.
	public static byte addressForPresentProperty(String name) {
		byte address = 0;
		if( name.equalsIgnoreCase(JointProperty.POSITION.name())) address = (byte)132;
		else {
			LOGGER.warning(String.format("%s.addressForPresentProperty: Unrecognized property name (%s)",CLSS,name));
		}
		return address;
	}
	// Return the data length for the named property in the control table. We assume that
	// present values and goals are the same number of bytes.
	// Valid for Protocol 1 only.
	public static byte dataBytesForProperty(String name) {
		byte length = 0;
		if( name.equalsIgnoreCase(JointProperty.POSITION.name())) length = 4;
		else if( name.equalsIgnoreCase(JointProperty.SPEED.name())) length = 4;
		else if( name.equalsIgnoreCase(JointProperty.TORQUE.name())) length = 2;
		else {
			LOGGER.warning(String.format("%s.dataBytesForProperty: Unrecognized property name (%s)",CLSS,name));
		}
		return length;
	}
}
