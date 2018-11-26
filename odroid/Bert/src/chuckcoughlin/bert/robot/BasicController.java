/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 *   The block controller is designed to be called from the client
 *   via RPC. All methods must be thread safe,
 */
package chuckcoughlin.bert.robot;

import java.util.List;

/**
 *  This is a rudimentary implementation of the Execution Controller interface. 
 *  It's function is as a stand-in for the true block execution controller in
 *  other environments separate from the Gateway. It does nothing but log warnings.
 */
public class BasicController implements Controller  {
	protected static final String CLSS = "BasicController";
	private final List<Motor> motors;

	public BasicController(List<Motor> motors) {
		this.motors = motors;
	}



	@Override
	public List<Motor> getMotors() {
		return motors;
	}}
