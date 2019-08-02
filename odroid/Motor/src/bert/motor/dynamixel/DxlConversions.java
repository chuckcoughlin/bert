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
import bert.share.motor.MotorConfiguration;

/**
 * This class contains methods used to convert between Dynamixel jointValues and engineering units.
 * Code is derived from Pypot dynamixel.conversion.py
 * Protocol 1 only.
 */
public class DxlConversions  {
	private static final String CLSS = "DynamixelConversions";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static Map<DynamixelType,Integer> range;         // Range of motion in degrees
	private static Map<DynamixelType,Integer> resolution;    // Increments for 360 deg by type
	public static final Map<DynamixelType,Double> torque;         // Max torque ~ Nm
	public static final Map<DynamixelType,Double> velocity;       // Angular velocity ~ deg/s
	// Constants for control table addressses. These must be the same for MX28,MX64,AX12
	public static final byte LIMIT_BLOCK_ADDRESS = (byte)6;
	public static final byte LIMIT_BLOCK_BYTES   = (byte)9;
	public static final byte GOAL_BLOCK_ADDRESS  = (byte)0x1E;
	public static final byte GOAL_BLOCK_BYTES    = (byte)6;
	public static final byte GOAL_POSITION       = (byte)0x1E;
	public static final byte GOAL_SPEED          = (byte)0x20;
	public static final byte GOAL_TORQUE         = (byte)0x22;
	public static final byte GOAL_TORQUE_ENABLE = (byte)0x18;
	private static final byte MINIMUM_ANGLE     = (byte)0x06;  // CCW
	private static final byte MAXIMUM_ANGLE     = (byte)0x08;  // CW
	private static final byte PRESENT_LOAD      = (byte)0x28;  // low, high bytes
	private static final byte PRESENT_POSITION  = (byte)0x24;  // low, high bytes
	private static final byte PRESENT_SPEED     = (byte)0x26;  // low, high bytes
	private static final byte PRESENT_TEMPERATURE = (byte)0x2B;  // single byte
	private static final byte PRESENT_VOLTAGE     = (byte)0x2A;  // single byte
	
	static {
		range = new HashMap<>();
		range.put(DynamixelType.AX12,300);
		range.put(DynamixelType.MX28,360); 
		range.put(DynamixelType.MX64,360); 
		
		resolution = new HashMap<>();
		resolution.put(DynamixelType.AX12,0x3FF);
		resolution.put(DynamixelType.MX28,0xFFF); 
		resolution.put(DynamixelType.MX64,0xFFF); 
		
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

	public int degreeToDxl(MotorConfiguration mc,double value) {
		if( value>mc.getMaxAngle()) {
			LOGGER.warning(String.format("%s.degreeToDxl: %s attempted move to %.0f (max = %.0f)",CLSS,mc.getJoint().name(),value,mc.getMaxAngle()));
			value = mc.getMaxAngle();
		}
		if( value<mc.getMinAngle()) {
			LOGGER.warning(String.format("%s.degreeToDxl: %s attempted move to %.0f (min = %.0f)",CLSS,mc.getJoint().name(),value,mc.getMinAngle()));
			value = mc.getMinAngle();
		}
		mc.setPosition(value);
		value = value - mc.getOffset();
		int r = range.get(mc.getType());
		if( !mc.isDirect() ) value = r - value;
		int res = resolution.get(mc.getType());
		int val = (int)(value*res/r);
		val = val&res;
		LOGGER.info(String.format("%s.degreeToDxl: %s b1,b2: %02X,%02X, offset %.0f %s",CLSS,mc.getJoint().name(),
				(byte)(val >>8),val&0xFF,mc.getOffset(),(mc.isDirect()?"DIRECT":"INDIRECT")));
		return val;
	}
	// The range in degrees is split in increments by resolution. 180deg is "up" when looking at
	// the motor head-on. Zero is at the bottom. Zero degrees is not possible for an AX-12.
	// CW is < 180 deg. CCW > 180deg
	public double dxlToDegree(MotorConfiguration mc,byte b1,byte b2) {
		int raw = (b1&0XFF) + 256*(b2&0XFF);
		int res = resolution.get(mc.getType());
		raw = raw & res;
		int r = range.get(mc.getType());
		
		double result = (double)raw*r/res;
		if( !mc.isDirect() ) result = r - result;
		result = result + mc.getOffset();
		//LOGGER.info(String.format("%s.dxlToDegree: %s b1,b2: %02X,%02X, offset %.0f %s result %.0f",CLSS,mc.getName().name(),b1,b2,
		//		mc.getOffset(),(mc.isDirect()?"DIRECT":"INDIRECT"),result));
		return result;
	}
	// Speed is deg/sec
	public int speedToDxl(MotorConfiguration mc,double value) {
		boolean cw = mc.isDirect();
		if( value<0.) cw = !cw;
		int val = (int)(value*1023./velocity.get(mc.getType()));
		if( !cw ) val = val | 0x400;
		return val;
	}
	public double dxlToSpeed(MotorConfiguration mc,byte b1,byte b2) {
		int raw = (b1&0XFF) + 256*(b2&0XFF);
		boolean cw = mc.isDirect();
		if( (raw & 0x400) != 0 ) cw = !cw;
		raw = raw&0x3FF;
		double result = raw*velocity.get(mc.getType())/1023;
		if(!cw) result = -result;
		return result;
	}
	
	// N-m (assumes EEPROM max torque is 1023)
	// Positive number is CCW. Each unit represents .1%.
	public int torqueToDxl(MotorConfiguration mc,double value) {
		boolean cw = mc.isDirect();
		if( value<0.) cw = !cw;
		int val = (int)(value*1023./torque.get(mc.getType()));
		if( cw ) val = val | 0x400;
		return val; 
	}
	// For a load the direction is pertinent. Positive implies CW.
	public double dxlToLoad(MotorConfiguration mc,byte b1,byte b2) {
		int raw = (b1&0XFF) + 256*(b2&0XFF);
		boolean cw = mc.isDirect();
		if( (raw & 0x400) != 0 ) cw = !cw;
		raw = raw&0x3FF;
		double result = raw*torque.get(mc.getType())/1023.;
		if(cw) result = -result;
		return result;
	}
	// The torque-limit values in the EEPROM don't make sense. AX-12 has 8C FF. M-28/64 A0 FF.
	// For these values we'll just return the spec limit. Limit/goals are always positive.
	public double dxlToTorque(MotorConfiguration mc,byte b1,byte b2) {
		double result = torque.get(mc.getType());   // Spec value
		if( b2!=0xFF ) {
			int raw = b1+256*b2;
			raw = raw&0x3FF;
			result = raw*result/0x3FF;
		}
		return result;
	}
	// This is really a boolean. Take 0.0 to be false, 1.0 to be true
	public double dxlToTorqueEnable(MotorConfiguration mc,byte b1) {
		double result = 1.0;
		if( b1==0x0 ) result = 1.0;
		return result;
	}
	// deg C
	public double dxlToTemperature(byte value) { return (double)value; }
	// volts
	public double dxlToVoltage(byte value) { return ((double)value)/10.; }

	// Convert the named property to a control table address for the present state
	// of that property. These need to  be independent of motor type.
	public byte addressForGoalProperty(String name) {
		byte address = 0;
		if( name.equalsIgnoreCase(JointProperty.POSITION.name())) address = GOAL_POSITION;
		else if( name.equalsIgnoreCase(JointProperty.SPEED.name())) address = GOAL_SPEED;
		else if( name.equalsIgnoreCase(JointProperty.TORQUE.name())) address = GOAL_TORQUE;
		else if( name.equalsIgnoreCase(JointProperty.TORQUE_ENABLE.name())) address = GOAL_TORQUE_ENABLE;
		else {
			LOGGER.warning(String.format("%s.addressForProperty: Unrecognized property name (%s)",CLSS,name));
		}
		return address;
	}
	// Convert the named property to a control table address for the present state
	// of that property. These need to  be independent of motor type.
	public byte addressForPresentProperty(String name) {
		byte address = 0;
		if( name.equalsIgnoreCase(JointProperty.MAXIMUMANGLE.name()))      address = MAXIMUM_ANGLE;
		else if( name.equalsIgnoreCase(JointProperty.MINIMUMANGLE.name())) address = MINIMUM_ANGLE;
		else if( name.equalsIgnoreCase(JointProperty.POSITION.name())) address = PRESENT_POSITION;
		else if( name.equalsIgnoreCase(JointProperty.SPEED.name())) address = PRESENT_SPEED;
		else if( name.equalsIgnoreCase(JointProperty.TEMPERATURE.name())) address = PRESENT_TEMPERATURE;
		else if( name.equalsIgnoreCase(JointProperty.TORQUE.name())) address = PRESENT_LOAD;
		else if( name.equalsIgnoreCase(JointProperty.TORQUE_ENABLE.name())) address = GOAL_TORQUE_ENABLE;
		else if( name.equalsIgnoreCase(JointProperty.VOLTAGE.name())) address = PRESENT_VOLTAGE;
		else {
			LOGGER.warning(String.format("%s.addressForProperty: Unrecognized property name (%s)",CLSS,name));
		}
		return address;
	}
	// Return the data length for the named property in the control table. We assume that
	// present values and goals are the same number of bytes.
	// Valid for Protocol 1 only.
	public byte dataBytesForProperty(String name) {
		byte length = 0;
		if( name.equalsIgnoreCase(JointProperty.MAXIMUMANGLE.name())) length = 2;
		else if( name.equalsIgnoreCase(JointProperty.MINIMUMANGLE.name())) length = 2;
		else if( name.equalsIgnoreCase(JointProperty.POSITION.name())) length = 2;
		else if( name.equalsIgnoreCase(JointProperty.SPEED.name())) length = 2;
		else if( name.equalsIgnoreCase(JointProperty.TEMPERATURE.name())) length = 1;
		else if( name.equalsIgnoreCase(JointProperty.TORQUE.name())) length = 2;
		else if( name.equalsIgnoreCase(JointProperty.TORQUE_ENABLE.name())) length = 1;
		else if( name.equalsIgnoreCase(JointProperty.VOLTAGE.name())) length = 1;
		else {
			LOGGER.warning(String.format("%s.dataBytesForProperty: Unrecognized property name (%s)",CLSS,name));
		}
		return length;
	}
	// Convert the value into a raw setting for the motor. Position is in degrees, speed and torque are percent.
	// Valid for Protocol 1 only.
	public int dxlValueForProperty(String name,MotorConfiguration mc,double value) {
		int dxlValue = 0;
		if( name.equalsIgnoreCase(JointProperty.POSITION.name())) dxlValue = degreeToDxl(mc,value);
		else if( name.equalsIgnoreCase(JointProperty.SPEED.name())) {
			value = value*mc.getMaxSpeed()/100.;
			dxlValue =speedToDxl(mc,value);
		}
		else if( name.equalsIgnoreCase(JointProperty.TORQUE.name())) {
			value = value*mc.getMaxTorque()/100.;
			dxlValue = torqueToDxl(mc,value);
		}
		else if( name.equalsIgnoreCase(JointProperty.TORQUE_ENABLE.name())) {
			dxlValue = 1;
			if(value==0.0) dxlValue = 0; 
		}
		return dxlValue;
	}
	// Convert the raw data bytes text describing the value and units. It may or may not use the second byte.
	// Presumably the ultimate will have more context.
	public String textForProperty(String name,MotorConfiguration mc,byte b1,byte b2) {
		name = name.toLowerCase();
		String text = "";
		double value = valueForProperty(name,mc,b1,b2);
		if( name.equalsIgnoreCase(JointProperty.MAXIMUMANGLE.name())) text = String.format("%.0f degrees",value);
		else if( name.equalsIgnoreCase(JointProperty.MINIMUMANGLE.name())) text = String.format("%.0f degrees",value);
		else if( name.equalsIgnoreCase(JointProperty.POSITION.name())) text = String.format("%.0f degrees",value);
		else if( name.equalsIgnoreCase(JointProperty.SPEED.name())) text = String.format("%.0f degrees per second",value);
		else if( name.equalsIgnoreCase(JointProperty.TEMPERATURE.name())) text = String.format("%.0f degrees centigrade",value);
		else if( name.equalsIgnoreCase(JointProperty.TORQUE.name())) text = String.format("%.1f newton-meters",value);
		else if( name.equalsIgnoreCase(JointProperty.TORQUE_ENABLE.name())) text = String.format("torque is %s",(value==0.?"disabled":"enabled"));
		else if( name.equalsIgnoreCase(JointProperty.VOLTAGE.name()))text = String.format("%.1f volts",value);
		else {
			LOGGER.warning(String.format("%s.textForProperty: Unrecognized property name (%s)",CLSS,name));
		}
		return text;
	}
	// Convert the raw data bytes into a double value. It may or may not use the second byte.
	// Value is engineering units.
	// Valid for Protocol 1 only.
	public double valueForProperty(String name,MotorConfiguration mc,byte b1,byte b2) {
		double value = 0.;
		if( name.equalsIgnoreCase(JointProperty.MAXIMUMANGLE.name())) value = dxlToDegree(mc,b1,b2);
		else if( name.equalsIgnoreCase(JointProperty.MINIMUMANGLE.name())) value = dxlToDegree(mc,b1,b2);
		else if( name.equalsIgnoreCase(JointProperty.POSITION.name())) value = dxlToDegree(mc,b1,b2);
		else if( name.equalsIgnoreCase(JointProperty.SPEED.name())) value = dxlToSpeed(mc,b1,b2);
		else if( name.equalsIgnoreCase(JointProperty.TEMPERATURE.name())) value = dxlToTemperature(b1);
		else if( name.equalsIgnoreCase(JointProperty.TORQUE.name())) value = dxlToTorque(mc,b1,b2);
		else if( name.equalsIgnoreCase(JointProperty.TORQUE_ENABLE.name())) value = dxlToTorqueEnable(mc,b1);
		else if( name.equalsIgnoreCase(JointProperty.VOLTAGE.name())) value = dxlToVoltage(b1);
		else {
			LOGGER.warning(String.format("%s.valueForProperty: Unrecognized property name (%s)",CLSS,name));
		}
		return value;
	}
}
