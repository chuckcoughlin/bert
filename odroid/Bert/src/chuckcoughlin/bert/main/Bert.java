/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.main;

import java.lang.System.Logger.Level;
import java.util.Iterator;
import java.util.ServiceLoader;

import chuckcoughlin.bert.robot.Humanoid;
import chuckcoughlin.logging.SyslogLogger;



public class Bert {
	private final static String CLSS = "Bert";
	private static final String USAGE = "Usage: Bert";
	private static System.Logger LOGGER = System.getLogger("Bert");
	private final Humanoid robot;
	
	
	public Bert() {
		this.robot = Humanoid.getInstance();
	}


	/**
	 * Entry point for the application that contains the robot Java
	 * code for control of the appendages. 
	 * 
	 * Usage: bert_runner 
	 * 
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
			
		// No arguments for the time being
		if( args.length < 10) {
			LOGGER.log(Level.INFO, USAGE);
			System.exit(1);
		}
		// Our custom logger as a service
		ServiceLoader<SyslogLogger> loader = ServiceLoader.load(SyslogLogger.class);
		Iterator<SyslogLogger> iter = loader.iterator();
		
		// -D log.level
		String levelString = System.getProperty("log.level");
		Level level = Level.INFO;
		if( levelString!=null ) level = Level.valueOf(levelString);
		while( iter.hasNext()) {
			SyslogLogger logger = iter.next();
		}
		
        Bert runner = new Bert();

  
	}

}
