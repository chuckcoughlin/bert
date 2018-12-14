/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package chuckcoughlin.bert.controller;

import java.util.ArrayList;
import java.util.List;

import chuckcoughlin.bert.common.DynamixelType;
import chuckcoughlin.bert.common.Joint;
import chuckcoughlin.bert.model.Motor;

/**
 *  A command controller is a server-side command handler. On receipt of
 *  a request, it posts the ...
 */
public class CommandController implements Controller  {
	protected static final String CLSS = "LowerController";
	private final List<Motor> motors;

	public CommandController(List<Motor> motors) {
		this.motors = new ArrayList<>();
	}



	@Override
	public List<Motor> getMotors() {
		return motors;
	}
}
