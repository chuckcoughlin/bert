/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License. 
 */
package chuckcoughlin.bert.controller;

import java.util.ArrayList;
import java.util.List;

import chuckcoughlin.bert.common.DynamixelType;
import chuckcoughlin.bert.common.Joint;
import chuckcoughlin.bert.model.Motor;

/**
 *  Holds a list of the motors for the torso, arms and head.
 */
public class UpperController implements Controller  {
	protected static final String CLSS = "UpperController";
	private final List<Motor> motors;

	public UpperController(List<Motor> motors) {
		this.motors = new ArrayList<>();
		populateMotors(motors);
	}



	@Override
	public List<Motor> getMotors() {
		return motors;
	}

	private void populateMotors(List<Motor> motors) {
		motors.add(new Motor(Joint.ABS_X,DynamixelType.MX64,32,0,-45.,45,false));
		motors.add(new Motor(Joint.ABS_Y,DynamixelType.MX64,31,0,-50,12,false));
		motors.add(new Motor(Joint.ABS_Z,DynamixelType.MX28,33,0,-90,90,true));
		motors.add(new Motor(Joint.BUST_X,DynamixelType.MX28,35,0,-40,40,false));
		motors.add(new Motor(Joint.BUST_Y,DynamixelType.MX28,34,0,-67,27,false));
		motors.add(new Motor(Joint.HEAD_Y,DynamixelType.MX28,37,20,-45,6,false));
		motors.add(new Motor(Joint.HEAD_Z,DynamixelType.AX12,36,0,-90,90,true));
		motors.add(new Motor(Joint.LEFT_ARM_Z,DynamixelType.MX28,43,0,-105,105,false));
		motors.add(new Motor(Joint.LEFT_ELBOW_Y,DynamixelType.MX28,44,0,148,1,true));
		motors.add(new Motor(Joint.LEFT_SHOULDER_X,DynamixelType.MX28,42,-90,-10,110,false));
		motors.add(new Motor(Joint.LEFT_SHOULDER_Y,DynamixelType.MX28,41,90,-120,155,true));
		motors.add(new Motor(Joint.RIGHT_ARM_Z,DynamixelType.MX28,53,0,-105,05,false));
		motors.add(new Motor(Joint.RIGHT_ELBOW_Y,DynamixelType.MX28,54,0,-1,148,false));
		motors.add(new Motor(Joint.RIGHT_SHOULDER_X,DynamixelType.MX28,52,90,-110,105,false));
		motors.add(new Motor(Joint.RIGHT_SHOULDER_Y,DynamixelType.MX28,51,90,-15,120,false));
	}
}
