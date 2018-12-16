/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License. 
 */
package bert.share.controller;

import java.util.List;

import bert.share.motor.Motor;

/**
 *  A Joint controller receives positioning commands from the server for a group
 *  of motors.
 */
public class JointController extends AbstractController implements Controller  {
	protected static final String CLSS = "JointController";
	private final List<Motor> motors;

	public JointController(List<Motor> ms) {
		this.motors = ms;
	}
	
	public List<Motor> getMotors() {
		return motors;
	}
}
