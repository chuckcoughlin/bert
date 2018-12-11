/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.loop.main;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;

import chuckcoughlin.bert.model.RobotModel;

public class ControlLoop {
	private final static String CLSS = "ControlLoop";
	private static final String USAGE = "Usage: loop <config-file>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private final RobotModel model;
	
	
	public ControlLoop(RobotModel m) {
		this.model = m;
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
		RobotModel model = new RobotModel(path);

		ControlLoop runner = new ControlLoop(model);

  
	}

}
