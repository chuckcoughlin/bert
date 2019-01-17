/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.motor.main;

import bert.share.bottle.MessageBottle;
import bert.share.controller.CommandController;

/**
 *  Handle requests directed to a specific group of motors. All motors in the 
 *  group are connected to the same serial port.
 */
public class PortHandler implements Runnable {
	protected static final String CLSS = "MotorGroupHandler";
	private final CommandController controller;
	private final String group;                 // Group name

	public PortHandler(String name,CommandController c) {
		this.controller = c;
		this.group = name;
	}

	public String getGroupName() { return this.group; }
	
	public void run() {
		while( !Thread.currentThread().isInterrupted() ) {
			MessageBottle work = controller.getMessage();
			sendSerialCommmand(work);
			MessageBottle status = getMotorState();
			controller.sendMessage(status);
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
