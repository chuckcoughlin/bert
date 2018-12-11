/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.bert.main;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;

import chuckcoughlin.bert.model.RobotModel;



public class Bert {
	private final static String CLSS = "Bert";
	private static final String USAGE = "Usage: bert <config-file>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private final RobotModel model;
	private final Humanoid robot;
	
	
	public Bert(RobotModel m) {
		this.robot = Humanoid.getInstance();
		this.model = m;
	}


	/**
	 * Entry point for the application that contains the robot Java
	 * code for control of the appendages. 
	 * 
	 * Usage: bert <config> 
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
		
        Bert runner = new Bert(model);

	}

}
