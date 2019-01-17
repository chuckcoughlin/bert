/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.motor.main;

import java.util.HashMap;
import java.util.Map;

import bert.share.bottle.MessageBottle;
import bert.share.common.Port;
import bert.share.motor.MotorConfiguration;

/**
 *  Handle requests directed to a specific group of motors. All motors in the 
 *  group are connected to the same serial port.
 */
public class PortHandler implements Runnable {
	protected static final String CLSS = "MotorGroupHandler";
	private final String group;                 // Group name
	private final Port port;
	private final Map<String,MotorConfiguration> configurations;

	public PortHandler(String name,Port p) {
		this.group = name;
		this.port = p;
		this.configurations = new HashMap<>();
	}

	public String getGroupName() { return this.group; }
	public MotorConfiguration getMotorConfiguration(String name) { return configurations.get(name); }
	public void putMotorConfiguration(String name,MotorConfiguration mc) {
		configurations.put(name, mc);
	}
	
	public void run() {
		while( !Thread.currentThread().isInterrupted() ) {
			/*
			MessageBottle work = controller.getMessage();
			sendSerialCommmand(work);
			MessageBottle status = getMotorState();
			controller.sendMessage(status);
			*/
		}
	}
	
	/**
	 * Request the current state of all motors in the group.
	 * @return a response bottle containg the stae information
	 */
	private MessageBottle getMotorState() {
		MessageBottle result = new MessageBottle();
		return result;
	}
	
	private void sendSerialCommmand(MessageBottle request) {
		
	}
}
