/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.term.main;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;

import chuckcoughlin.bert.model.RobotModel;



public class Terminal {
	private final static String CLSS = "Terminal";
	private static final String USAGE = "Usage: terminal <config-file>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private final RobotModel model;
	
	public Terminal(RobotModel m) {
		this.model = m;
	}


	/**
	 * Entry point for the application that contains the robot Java
	 * code for control of the appendages. 
	 * 
	 * Usage: term <config> 
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
		
        Terminal runner = new Terminal(model);
	}

}
