/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.command.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

import bert.command.model.Humanoid;
import bert.command.model.RobotCommandModel;
import bert.share.bottle.MessageBottle;
import bert.share.common.NamedPipePair;
import bert.share.common.PathConstants;
import bert.share.controller.CommandController;
import bert.share.controller.ControllerLauncher;
import bert.share.logging.LoggerUtility;
import bert.speech.process.StatementParser;
import bert.sql.db.Database;

/**
 * This is the main client class (on the controller side of the pipes). It holds
 * the command, playback, record and joint controllers. 
 */
public class Bert implements ControllerLauncher {
	private final static String CLSS = "Bert";
	private static final String USAGE = "Usage: bert <config-file>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	
	private final RobotCommandModel model;
	private CommandController controller = null;
	private final Humanoid robot;
	private final StatementParser parser;
	
	
	public Bert(RobotCommandModel m) {
		this.robot = Humanoid.getInstance();
		this.model = m;
		this.parser = new StatementParser();
	}

	/**
	 * This application is only interested in the command controller. There should only be
	 * one entry in the map.
	 */
	@Override
	public void createControllers() {
		Map<String, String> pipeNames = model.getPipeNames();
		Iterator<String>walker = pipeNames.keySet().iterator();
		String key = walker.next();
		String pipeName = pipeNames.get(key);
		NamedPipePair pipe = new NamedPipePair(pipeName,false);  // Not the "owner"
		this.controller = new CommandController(this,pipe,false);       // Asynchronous
	}
	
	/**
	 * Loop forever acquiring phrases from the Bluetooth connection to Android. Use ANTLR to convert into requests.
	 * Handle requests in a controller running in its own thread. Other controllers handle play and record plus
	 * the robot movements.
	 */
	/**
	 * Loop forever reading from the terminal. Use ANTLR to convert text into requests.
	 * Handle requests in a controller running in its own thread. Display responses.
	 */
	public void execute() {
		
		/**
		 * Read from Bluetooth ....
		 */
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(System.in));

			while (true) {
				String input = br.readLine();

				if( "q".equalsIgnoreCase(input)    ||
					"quit".equalsIgnoreCase(input) ||
					"exit".equalsIgnoreCase(input)    ) {
					break;
				}
				else if(input.isBlank()) continue;
				/*
				 * 1) Analyze the input string via ANTLR
				 * 2) Send the resulting RequestBottle to the TerminalController
				 *    The request may hang while the controller input buffer is full. 
				 * 3) On callback from the controller, convert ResponseBottle to english string
				 * 4) Send string to stdout
				 */
				else {
					MessageBottle request = parser.parseStatement(input);
					controller.sendMessage(request);
				}
			}

		} 
		catch (IOException ioe) {
			ioe.printStackTrace();
		} 
		catch (Exception ex) {
			ex.printStackTrace();
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
			controller.stop();
		}
		Database.getInstance().shutdown();
		System.exit(0);
	}

	@Override
	public void handleResult(MessageBottle response) {
		
	}
	
	/**
	 * Entry point for the application that contains the robot Java
	 * code for control of the appendages, among other things. 
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
		PathConstants.setHome(path);
		// Setup logging to use only a file appender to our logging directory
		LoggerUtility.getInstance().configureRootLogger();
		
		RobotCommandModel model = new RobotCommandModel(PathConstants.CONFIG_PATH);
		model.populate();
		Database.getInstance().startup(PathConstants.DB_PATH);
		Database.getInstance().populateMotors(model.getMotors());
        Bert runner = new Bert(model);
        runner.createControllers();
        runner.execute();

	}

}
