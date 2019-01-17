/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.main;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import bert.motor.model.RobotMotorModel;
import bert.share.bottle.MessageBottle;
import bert.share.common.NamedPipePair;
import bert.share.common.PathConstants;
import bert.share.controller.CommandController;
import bert.share.controller.ControllerLauncher;
import bert.share.logging.LoggerUtility;

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
	 * This application is interested in the "joint" controllers. We can expect multiple
	 * instances. They operate in synchronous fashion. Assign each to a handler running 
	 * in its own thread.
	 */
	public void createMotorGroups() {
		Map<String, String> pipeNames = model.getPipeNames();
		Iterator<String>walker = pipeNames.keySet().iterator();
		String key = walker.next();                              // Group name
		String pipeName = pipeNames.get(key);
		NamedPipePair pipe = new NamedPipePair(pipeName,false);  // Not the "owner"
		CommandController controller = new CommandController(this,pipe,true);  // Synchronous
		PortHandler handler = new PortHandler(key,controller);
		Thread thread = new Thread(handler);
		motorGroupHandlers.put(key, thread);
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
	
	/**
	 * This is never called since we are using synchronous interactions.
	 * @param response
	 */
	@Override
	public void handleResult(MessageBottle response) {}
	
}
