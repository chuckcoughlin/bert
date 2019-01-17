/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.dispatcher.main;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

import bert.dispatcher.model.RobotDispatcherModel;
import bert.motor.main.MotorManager;
import bert.motor.model.RobotMotorModel;
import bert.share.bottle.BottleConstants;
import bert.share.bottle.MessageBottle;
import bert.share.common.NamedPipePair;
import bert.share.common.PathConstants;
import bert.share.controller.ControllerType;
import bert.share.logging.LoggerUtility;

/**
 * The job of the distributer is to process entries from the request channels,
 * distribute them to action channels and post the results.
 */
public class Dispatcher {
	private final static String CLSS = "Distributer";
	private static final String USAGE = "Usage: dispatcher <robot_root>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private final RobotDispatcherModel model;
	private final CommandHandler commandHandler;
	private final CommandHandler terminalHandler;
	private final MotorManager   motorManager;
	private final String name;
	private int cadence = 1000;
	
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public Dispatcher(RobotDispatcherModel m,MotorManager mgr) {
		this.model = m;
		this.motorManager = mgr;
		this.commandHandler = new CommandHandler(this);
		this.terminalHandler = new CommandHandler(this);
		
		this.name = model.getProperty(BottleConstants.PROPERTY_NAME,"Bert");
		String cadenceString = model.getProperty(BottleConstants.VALUE_CADENCE,"1000");  // ~msecs
		try {
			this.cadence = Integer.parseInt(cadenceString);
		}
		catch(NumberFormatException nfe) {
			LOGGER.log(Level.WARNING,String.format("%s.constructor: Cadence must be an integer (%s)",CLSS,nfe.getLocalizedMessage()));
		}
		
	}
	
	/**
	 * The dispatcher creates controllers for the Terminal and Command applications. The 
	 * MotorManager does its own configuration (creates a port handler for each motor group).
	 */
	public void createControllers() {
		Map<String, String> pipeNames = model.getPipeNames();
		Iterator<String>walker = pipeNames.keySet().iterator();
		String key = walker.next();
		String pipeName = pipeNames.get(key);
		NamedPipePair pipe = new NamedPipePair(pipeName,true);  // Not the "owner"
		ControllerType type = ControllerType.valueOf(model.getControllerTypes().get(key));
		if( type.equals(ControllerType.COMMAND)) {
			commandHandler.setPipe(pipe); 
		}
		else if( type.equals(ControllerType.TERMINAL)) {
			terminalHandler.setPipe(pipe);
		}
			
	}
	
	public void execute() {
		for(;;) {
			MessageBottle request = null;
			MessageBottle response = null;
			// First try the commands in an asynchronous way,
			// Take care of any local requests (then immediately got to next command)
			if( terminalHandler.getLocalRequest()!=null ) {
				// Handle terminal local request - -create response
				response = createResponseForLocalRequest(terminalHandler.getLocalRequest());
				
			}
			else if( commandHandler.getLocalRequest()!=null ) {
				// Handle command local request - -create response
				response = createResponseForLocalRequest(commandHandler.getLocalRequest());
			}
			else {
				// Handle motor request
				motorManager.sendMessage(request);
				response = motorManager.getMessage();
			}
			
			String source = request.getSource();
			// Return response to the request source.
			
			// Delay until cadence interval is up.
			
		}
	}
	
	private MessageBottle createResponseForLocalRequest(MessageBottle request) {
		return null;
	}

	/**
	 * Entry point for the application that contains the robot Java
	 * code for control of the appendages. 
	 * 
	 * Usage: Usage: loop <config> 
	 * 
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
			
		// Make sure there is command-line argument
		if( args.length < 1) {
			LOGGER.log(Level.INFO, USAGE);
			System.exit(1);
		}


		// Analyze command-line argument to obtain the configuration file path.
		String arg = args[0];
		Path path = Paths.get(arg);
		PathConstants.setHome(path);
		// Setup logging to use only a file appender to our logging directory
		LoggerUtility.getInstance().configureRootLogger();
		
		
		RobotMotorModel mmodel = new RobotMotorModel(PathConstants.CONFIG_PATH);
		mmodel.populate();    // Analyze the xml for motor groups
		MotorManager mgr = new MotorManager(mmodel);
		mgr.createMotorGroups();
		
		RobotDispatcherModel model = new RobotDispatcherModel(PathConstants.CONFIG_PATH);
		model.populate();    // Analyze the xml
		Dispatcher runner = new Dispatcher(model,mgr);
		runner.createControllers();
		
		runner.execute();

  
	}

}
