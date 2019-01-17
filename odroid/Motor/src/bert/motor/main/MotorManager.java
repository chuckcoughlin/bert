/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.main;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bert.motor.model.RobotMotorModel;
import bert.share.bottle.MessageBottle;
import bert.share.common.NamedPipePair;
import bert.share.common.Port;
import bert.share.controller.CommandController;
import bert.share.motor.MotorConfiguration;

/**
 * The MotorHandler receives requests across the pipe from the dispatcher,
 * formulates a serial command and delivers it to the motors. It then requests
 * motor status and formulates the reply.
 * 
 * Each controller handles a group of motors that are all on the same
 * serial port. For each request there is a single response. Responses
 * are synchronous.
 */
public class MotorManager {
	private final static String CLSS = "MotorHandler";
	private static final String USAGE = "Usage: motors <robot_root>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private final RobotMotorModel model;
	private final Map<String,Thread> motorGroupHandlers;

	
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public MotorManager(RobotMotorModel m) {
		this.model = m;
		this.motorGroupHandlers = new HashMap<>();
		
	}

	/**
	 * This application is interested in the "serial" controllers. We can expect multiple
	 * instances. Assign each to a handler running in its own thread. The handler is aware
	 * of the individual motors in the group.
	 */
	public void createMotorGroups() {
		Set<String> groupNames = model.getControllerTypes().keySet();
		Map<String,MotorConfiguration> motors = model.getMotors(); 
		for( String group:groupNames ) {
			Port port = model.getPortForGroup(group);
			PortHandler handler = new PortHandler(group,port);
			Thread t = new Thread(handler);
			motorGroupHandlers.put(group, t);
			
			// Add all the motor configurations to the handler
			List<String> jointNames = model.getJointNamesForGroup(group);
			for( String jname:jointNames ) {
				MotorConfiguration motor = motors.get(jname.toUpperCase());
				handler.putMotorConfiguration(jname, motor);
			}
		}
	}
	
	public void start() {
		for( Thread t:motorGroupHandlers.values()) {
			t.start();
		}
	}

	/** 
	 * Who calls this?
	 */
	public void stop() {
		for( Thread t:motorGroupHandlers.values()) {
			t.interrupt();
		}
	}
	
	public MessageBottle getMessage() {
		MessageBottle response = null;
		return response;
	}

	public void sendMessage(MessageBottle request) {
		
	}

	
}
