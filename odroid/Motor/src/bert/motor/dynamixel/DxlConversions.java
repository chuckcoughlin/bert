/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.motor.dynamixel;

import java.util.HashMap;
import java.util.Map;

import bert.share.common.DynamixelType;

/**
 * This class contains static methods used to convert between Dynamixel values and engineering units.
 * Code is derived from Pypot dynamixel.conversion.py
 */
public class DxlConversions  {
	private static final String CLSS = "DynamixelConversions";
	private static Map<DynamixelType,Integer> resolution;         // Increments for 360 deg by type
	private static Map<DynamixelType,Double> torque;              // Max torque ~ Nm
	private static Map<DynamixelType,Double> velocity;            // Angular velocity ~ deg/s
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
}
