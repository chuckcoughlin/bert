/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.server.main;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;

import bert.server.model.RobotServerModel;
import bert.share.bottle.BottleConstants;
import bert.share.common.RobotConstants;

/**
 * The ControlLoop is the main server-side class.
 *
 */
public class ControlLoop  {
	private final static String CLSS = "ControlLoop";
	private static final String USAGE = "Usage: loop <config-file>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private final RobotServerModel model;
	private final String name;
	private int cadence = 1000;
	
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public ControlLoop(RobotServerModel m) {
		this.model = m;
		this.name = model.getProperty(BottleConstants.PROPERTY_NAME,RobotConstants.ROBOT_NAME);
		String cadenceString = model.getProperty(BottleConstants.PROPERTY_CADENCE,"1000");  // ~msecs
		try {
			this.cadence = Integer.parseInt(cadenceString);
		}
		catch(NumberFormatException nfe) {
			LOGGER.log(Level.WARNING,String.format("%s.constructor: Cadence must be an integer (%s)",CLSS,nfe.getLocalizedMessage()));
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
		RobotServerModel model = new RobotServerModel(path);
		model.populate();    // Analyze the xml

		ControlLoop runner = new ControlLoop(model);

  
	}

}
