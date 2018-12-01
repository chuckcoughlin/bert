/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 *   The block controller is designed to be called from the client
 *   via RPC. All methods must be thread safe,
 */
package chuckcoughlin.bert.robot;

import java.util.ArrayList;
import java.util.List;

import chuckcoughlin.bert.common.DynamixelType;
import chuckcoughlin.bert.common.Joint;

/**
 *  Holds a list of the motors for the legs.
 */
public class LowerController implements Controller  {
	protected static final String CLSS = "LowerController";
	private final List<Motor> motors;

	public LowerController(List<Motor> motors) {
		this.motors = new ArrayList<>();
		populateMotors(motors);
	}



	@Override
	public List<Motor> getMotors() {
		return motors;
	}
	
	private void populateMotors(List<Motor> motors) {
		motors.add(new Motor(Joint.LEFT_ANKLE_Y,DynamixelType.MX28,15,0,-45,45,true));
		motors.add(new Motor(Joint.LEFT_HIP_X,DynamixelType.MX28,11,0,-30,28.5,true));
		motors.add(new Motor(Joint.LEFT_HIP_Y,DynamixelType.MX64,13,2,-104,84,true));
		motors.add(new Motor(Joint.LEFT_HIP_Z,DynamixelType.MX28,2,0,-25,90,false));
		motors.add(new Motor(Joint.LEFT_KNEE_Y,DynamixelType.MX28,14,0,-3.5,134,true));
		motors.add(new Motor(Joint.RIGHT_ANKLE_Y,DynamixelType.MX28,25,0,-45,45,false));
		motors.add(new Motor(Joint.RIGHT_HIP_X,DynamixelType.MX28,21,0,-28.5,30,true));
		motors.add(new Motor(Joint.RIGHT_HIP_Y,DynamixelType.MX64,23,0,-85,105,false));
		motors.add(new Motor(Joint.RIGHT_HIP_Z,DynamixelType.MX28,22,0,-90,25,false));
		motors.add(new Motor(Joint.RIGHT_KNEE_Y,DynamixelType.MX28,24,0,-134,3.5,false));
	}
}
