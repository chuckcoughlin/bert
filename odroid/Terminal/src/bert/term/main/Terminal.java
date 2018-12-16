/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.term.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;

import bert.share.bottle.BottleConstants;
import bert.share.controller.Controller;
import bert.term.model.RobotTerminalModel;


/**
 * "Terminal" is an application that allows interaction from the command line
 * to command and interrogate the robot. Command entries are the same as
 * those given to the "headless" application, "Bert" in spoken form.
 */
public class Terminal implements Controller {
	private final static String CLSS = "Terminal";
	private static final String USAGE = "Usage: terminal <config-file>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private final RobotTerminalModel model;
	private String prompt;
	
	public Terminal(RobotTerminalModel m) {
		this.model = m;
		this.prompt = model.getProperty(BottleConstants.PROPERTY_PROMPT,"bert:");
	}
	
	public void run() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(System.in));

			while (true) {
				System.out.print(prompt);
				String input = br.readLine();

				if( "q".equalsIgnoreCase(input)    ||
					"quit".equalsIgnoreCase(input) ||
					"exit".equalsIgnoreCase(input)    ) {
					break;
				}
				else if(input.isBlank()) continue;
				/*
				 * 1) Analyze the input string via ANTLR
				 * 2) Send the resulting RequestBottle to the PipeHandler (in its own thread)
				 * 3) On callback from the PipeHandler, convert ResponseBottle to english string
				 * 4) Send string to stdout
				 */
				else {
					
				}
			}

		} 
		catch (IOException e) {
			e.printStackTrace();
		} 
		finally {
			if (br != null) {
				try {
					br.close();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		System.exit(0);
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
		RobotTerminalModel model = new RobotTerminalModel(path);
		model.populate();
		
        Terminal runner = new Terminal(model);
        runner.run();
	}

}
