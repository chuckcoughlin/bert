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
 *  A status controller is a server-side entity that ...
 */
public class StatusController implements Controller  {
	protected static final String CLSS = "UpperController";
	private final List<Motor> motors;

	public StatusController(List<Motor> motors) {
		this.motors = new ArrayList<>();
	}



	@Override
	public List<Motor> getMotors() {
		return motors;
	}

}
