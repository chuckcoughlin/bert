/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.dispatcher.main;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

import bert.dispatcher.model.RobotDispatcherModel;
import bert.share.bottle.BottleConstants;
import bert.share.bottle.MessageBottle;
import bert.share.common.NamedPipePair;
import bert.share.common.PathConstants;
import bert.share.controller.Controller;
import bert.share.controller.ControllerLauncher;
import bert.share.logging.LoggerUtility;

/**
 * The job of the distributer is to process entries from the request channels,
 * distribute them to action channels and post the results.
 */
public class Dispatcher implements ControllerLauncher {
	private final static String CLSS = "Distributer";
	private static final String USAGE = "Usage: dispatcher <robot_root>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private final RobotDispatcherModel model;
	private final CommandHandler commandHandler;
	private final ControllerHandler controllerHandler;
	private final String name;
	private int cadence = 1000;
	
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public Dispatcher(RobotDispatcherModel m) {
		this.model = m;
		this.commandHandler = new CommandHandler(this);
		this.controllerHandler = new ControllerHandler(this);
		
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
	 * The dispatcher creates controllers for all named pipes. Xome are considered inputs,
	 * others outputs (the motors).
	 */
	@Override
	public void createControllers() {
		Map<String, String> pipeNames = model.getPipeNames();
		Iterator<String>walker = pipeNames.keySet().iterator();
		String key = walker.next();
		String pipeName = pipeNames.get(key);
		ControllerType type = controllerTypes.get(key);
		NamedPipePair pipe = new NamedPipePair(pipeName,false);  // Not the "owner"
		Controller controller = new Controller(this,pipe,false);   // Asynchronous
		ControllerType type = controllerTypes.get(key);
	}
	
	public void execute() {
		for(;;) {
			// First try the commands in an asynchronous way,
			// Take care of any local requests (then immediately got to next command)
			if( terminalHandler.getLocalRequest()!=null ) {
				continue;
			}
			else if( commandHandler.getLocalRequest()!=null ) {
				continue;
			}
			
		}
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
		
		RobotDispatcherModel model = new RobotDispatcherModel(PathConstants.CONFIG_PATH);
		model.populate();    // Analyze the xml

		Dispatcher runner = new Dispatcher(model);
		runner.createControllers();
		runner.execute();

  
	}

}
