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
import bert.share.bottle.RequestBottle;
import bert.share.bottle.ResponseBottle;
import bert.share.common.PathConstants;
import bert.share.controller.ControllerLauncher;
import bert.share.logging.LoggerUtility;
import bert.speech.process.StatementParser;
import bert.term.model.RobotTerminalModel;


/**
 * "Terminal" is an application that allows interaction from the command line
 * to command and interrogate the robot. Command entries are the same as
 * those given to the "headless" application, "Bert" in spoken form.
 */
public class Terminal implements ControllerLauncher {
	private final static String CLSS = "Terminal";
	private static final String CONTROLLER_KEY = "TERMINAL_CONTROLLER";
	private static final String USAGE = "Usage: terminal <robot_root>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private final RobotTerminalModel model;
	private final TerminalController controller;
	private final StatementParser parser;
	private String prompt;
	
	public Terminal(RobotTerminalModel m) {
		this.model = m;
		this.prompt = model.getProperty(BottleConstants.PROPERTY_PROMPT,"bert:");
		this.controller = new TerminalController(CONTROLLER_KEY,this);
		this.parser = new StatementParser();
	}

	
	/**
	 * Loop forever reading from the terminal. Use ANTLR to convert into requests.
	 * Handle requests in a controller running in its own thread. Display responses.
	 */
	public void execute() {
		controller.configure(model);
		Thread controllerThread = new Thread(controller);
		controllerThread.start();
		
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
				 * 2) Send the resulting RequestBottle to the TerminalController
				 *    The request may hang while the controller input buffer is full. 
				 * 3) On callback from the controller, convert ResponseBottle to english string
				 * 4) Send string to stdout
				 */
				else {
					RequestBottle request = parser.parseStatement(input);
					controller.submitRequest(request);
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
			controllerThread.interrupt();
		}
		System.exit(0);
	}

	@Override
	public void handleResult(String key, ResponseBottle response) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Entry point for the application that allows direct user input through
	 * stdio. The argument specifies a directory that is the root of the various
	 * robot configuration, code and devices.
	 * 
	 * Usage: term <bert_root> 
	 * 
	 * @param args command-line arguments. Only one matters.
	 */
	public static void main(String[] args) {
			
		// Make sure there is command-line argument
		if( args.length < 1) {
			LOGGER.log(Level.INFO, USAGE);
			System.exit(1);
		}

		// Analyze command-line argument to obtain the file path to BERT_HOME.
		String arg = args[0];
		Path path = Paths.get(arg);
		PathConstants.setHome(path);
		// Setup logging to use only a file appender to our logging directory
		LoggerUtility.getInstance().configureRootLogger();
		
		RobotTerminalModel model = new RobotTerminalModel(PathConstants.CONFIG_PATH);
		model.populate();
		
		Terminal runner = new Terminal(model);
        runner.execute();
	}

}
