/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.dispatcher.main;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;

import bert.dispatcher.model.RobotDispatcherModel;
import bert.motor.main.MotorManager;
import bert.motor.model.RobotMotorModel;
import bert.share.bottle.BottleConstants;
import bert.share.bottle.MessageBottle;
import bert.share.bottle.MetricType;
import bert.share.bottle.RequestType;
import bert.share.common.NamedPipePair;
import bert.share.common.PathConstants;
import bert.share.controller.ControllerType;
import bert.share.logging.LoggerUtility;
import bert.share.model.ConfigurationConstants;

/**
 * The job of the distributer is to process entries from the request channels,
 * distribute them to action channels and post the results.
 */
public class Dispatcher {
	private final static String CLSS = "Distributer";
	private static final String USAGE = "Usage: dispatcher <robot_root>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private double WEIGHT = 0.5;  // weighting to give previous in EWMA
	private final RobotDispatcherModel model;
	private DispatchController commandController;
	private DispatchController terminalController;
	private final MotorManager   motorManager;
	private final String name;
	private int cadence = 1000;      // msecs
	private	double cycleTime = 0.0; // msecs,    EWMA
	private double dutyCycle = 0.0; // fraction, EWMA
	
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public Dispatcher(RobotDispatcherModel m,MotorManager mgr) {
		this.model = m;
		this.motorManager = mgr;
		
		this.name = model.getProperty(ConfigurationConstants.PROPERTY_NAME,"Bert");
		String cadenceString = model.getProperty(ConfigurationConstants.PROPERTY_CADENCE,"1000");  // ~msecs
		try {
			this.cadence = Integer.parseInt(cadenceString);
		}
		catch(NumberFormatException nfe) {
			LOGGER.log(Level.WARNING,String.format("%s.constructor: Cadence must be an integer (%s)",CLSS,nfe.getLocalizedMessage()));
		}
		
	}
	
	/**
	 * The dispatcher creates controllers for the Terminal and Command applications. The 
	 * MotorManager does its own configuration (creates a port handler for each motor group).
	 * 
	 */
	public void createControllers() {
		Map<String, String> pipeNames = model.getPipeNames();
		Iterator<String>walker = pipeNames.keySet().iterator();
		String key = walker.next();
		String pipeName = pipeNames.get(key);
		NamedPipePair pipe = new NamedPipePair(pipeName,true);  // Dispatcher is the "owner"
		pipe.create();                                          // Create the pipe if it doesn't exist
		ControllerType type = ControllerType.valueOf(model.getControllerTypes().get(key));
		if( type.equals(ControllerType.COMMAND)) {
			commandController = new DispatchController(pipe); 
		}
		else if( type.equals(ControllerType.TERMINAL)) {
			terminalController = new DispatchController(pipe); 
		}
			
	}
	
	public void execute() {
		try {
			commandController.start();
			terminalController.start();

			for(;;) {
				long startCycle = System.currentTimeMillis();
				MessageBottle request = null;
				MessageBottle response = null;
				// First try the commands in an asynchronous way,
				request = terminalController.getMessage();
				if( request==null) request = commandController.getMessage();

				// Take care of any local requests (then immediately go to next command)
				if( request!=null ) {
					if( isLocalRequest(request) ) {
						// Handle terminal local request - -create response
						response = createResponseForLocalRequest(request);
						sendResponse(response);
						continue;  // Bypass wait for cadence
					}
					else {
						// Handle motor request
						response = motorManager.processRequest(request);
						sendResponse(response);
					}
				}
				// Delay until cadence interval is up.
				long endCycle = System.currentTimeMillis();
				long elapsed = (endCycle-startCycle);
				this.cycleTime = exponentiallyWeightedMovingAverage(this.cycleTime,elapsed);
				this.dutyCycle = exponentiallyWeightedMovingAverage(this.dutyCycle,(double)elapsed/cadence);
				if( elapsed<cadence) {
					try {
						Thread.sleep(cadence-elapsed);
					}
					catch(InterruptedException ignore) {}
				}
			}
		}  
		catch (Exception ex) {
			ex.printStackTrace();
		} 
		finally {
			commandController.stop();
			terminalController.stop();
			motorManager.stop();
		}
		System.exit(0);
	}
	
	// The "local" response is simply the original request with some text
	// to send directly to the user.
	private MessageBottle createResponseForLocalRequest(MessageBottle request) {
		if( request.getRequestType().equals(RequestType.GET_METRIC)) {
			MetricType metric = MetricType.valueOf(request.getProperty(BottleConstants.PROPERTY_METRIC, "NAME"));
			String text = "";
			switch(metric) {
				case AGE:
					LocalDate today = LocalDate.now();
					LocalDate birthday = LocalDate.of(2019, Month.JANUARY, 1);

					Period p = Period.between(birthday, today);
					text = "I am " + p.getYears() + " years, " + p.getMonths() +
					                   " months, and " + p.getDays() +
					                   " days old";
					break;
				case CADENCE:
					text = "The cadence is "+this.cadence+" milliseconds";
					break;
				case CYCLETIME:
					text = "The average cycle time is "+(int)this.cycleTime+" milliseconds";
					break;
				case DUTYCYCLE:
					text = "My average duty cycle is "+(int)(100.*this.dutyCycle)+" percent";
					break;
				case HEIGHT:
					text = "My height when standing is 83 centimeters";
					break;
				case NAME:
					text = "My name is "+this.name;
					break;
			}
			request.setProperty(BottleConstants.TEXT, text);
		}
		return request;
	}

	private double exponentiallyWeightedMovingAverage(double currentValue,double previousValue) {
		double result = (1.-WEIGHT)*currentValue + WEIGHT*previousValue;
		return result;
	}
	
	private boolean isLocalRequest(MessageBottle request) {
		if( request.getRequestType().equals(RequestType.GET_METRIC)) {
			return true;
		}
		return false;
	}
	
	// Return response to the request source.
	private void sendResponse(MessageBottle response) {
		String source = response.getSource();
		if( source.equalsIgnoreCase(ControllerType.COMMAND.name())) {
			commandController.sendMessage(response);
		}
		else if( source.equalsIgnoreCase(ControllerType.TERMINAL.name())) {
			terminalController.sendMessage(response);
		}
	}
	
	/**
	 * Entry point for the dispatcher application that receives commands, processes
	 * them through the serial interfaces to the motors and returns results. 
	 * 
	 * Usage: Usage: dispatch <robot_root> 
	 * 
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
			
		// Make sure there is command-line argument
		if( args.length < 1) {
			LOGGER.log(Level.INFO, USAGE);
			System.exit(1);
		}


		// Analyze command-line argument to obtain the robot root directory.
		String arg = args[0];
		Path path = Paths.get(arg);
		PathConstants.setHome(path);
		// Setup logging to use only a file appender to our logging directory
		LoggerUtility.getInstance().configureRootLogger();
		
		
		RobotMotorModel mmodel = new RobotMotorModel(PathConstants.CONFIG_PATH);
		mmodel.populate();    // Analyze the xml for motor groups
		MotorManager mgr = new MotorManager(mmodel);
		mgr.createMotorGroups();
		
		RobotDispatcherModel model = new RobotDispatcherModel(PathConstants.CONFIG_PATH);
		model.populate();    // Analyze the xml
		Dispatcher runner = new Dispatcher(model,mgr);
		runner.createControllers();
		
		runner.execute();

  
	}

}
