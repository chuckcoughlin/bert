/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.main;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;

import bert.motor.model.RobotMotorModel;
import bert.share.bottle.BottleConstants;
import bert.share.bottle.ResponseBottle;
import bert.share.common.PathConstants;
import bert.share.controller.ControllerLauncher;
import bert.share.logging.LoggerUtility;

/**
 * The job of the distributer is to process entries from the request channels,
 * distrubute them to action channels and post the results.
 */
public class Distributer implements ControllerLauncher {
	private final static String CLSS = "ControlLoop";
	private static final String USAGE = "Usage: loop <config-file>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private final RobotMotorModel model;
	private final String name;
	private int cadence = 1000;
	
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public Distributer(RobotMotorModel m) {
		this.model = m;
		this.name = model.getProperty(BottleConstants.PROPERTY_NAME,"Bert");
		String cadenceString = model.getProperty(BottleConstants.VALUE_CADENCE,"1000");  // ~msecs
		try {
			this.cadence = Integer.parseInt(cadenceString);
		}
		catch(NumberFormatException nfe) {
			LOGGER.log(Level.WARNING,String.format("%s.constructor: Cadence must be an integer (%s)",CLSS,nfe.getLocalizedMessage()));
		}
		
	}

	public void execute() {
		
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
		
		RobotMotorModel model = new RobotMotorModel(PathConstants.CONFIG_PATH);
		model.populate();    // Analyze the xml

		Distributer runner = new Distributer(model);
		runner.execute();

  
	}


	@Override
	public void handleResult(String key, ResponseBottle response) {
		// TODO Auto-generated method stub
		
	}

}
