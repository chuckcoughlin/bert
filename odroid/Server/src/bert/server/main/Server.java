/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.server.main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import bert.motor.main.MotorGroupController;
import bert.motor.model.RobotMotorModel;
import bert.server.model.RobotDispatcherModel;
import bert.share.common.PathConstants;
import bert.share.controller.SocketController;
import bert.share.controller.SocketStateChangeEvent;
import bert.share.controller.SocketStateChangeListener;
import bert.share.controller.TimerController;
import bert.share.logging.LoggerUtility;
import bert.share.message.BottleConstants;
import bert.share.message.HandlerType;
import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;
import bert.share.message.MetricType;
import bert.share.message.RequestType;
import bert.share.model.ConfigurationConstants;
import bert.share.util.ShutdownHook;

/**
 * The dispatcher is its own system process. It's job is to accept requests from 
 * the command pipes, distribute them to the motor manager channels and post the results.
 */
public class Server implements MessageHandler,SocketStateChangeListener {
	private final static String CLSS = "Server";
	private static final String USAGE = "Usage: server <robot_root>";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static final String LOG_ROOT = "server";
	private double WEIGHT = 0.5;  // weighting to give previous in EWMA
	private final RobotDispatcherModel model;
	private SocketController commandController = null;
	private SocketController terminalController= null;
	private TimerController timerController       = null;
	private MotorGroupController motorGroupController = null;
	private final Condition busy;
	private MessageBottle currentRequest = null;
	private final Lock lock;
	
	private final String name;
	private int cadence = 1000;     // msecs
	private	int cycleCount = 0;     // messages processed
	private	double cycleTime = 0.0; // msecs,    EWMA
	private double dutyCycle = 0.0; // fraction, EWMA
	
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public Server(RobotDispatcherModel m,MotorGroupController mgc) {
		this.model = m;
		this.lock = new ReentrantLock();
		this.busy = lock.newCondition();
		this.motorGroupController = mgc;
		this.name = model.getProperty(ConfigurationConstants.PROPERTY_ROBOT_NAME,"Bert");
		String cadenceString = model.getProperty(ConfigurationConstants.PROPERTY_CADENCE,"1000");  // ~msecs
		try {
			this.cadence = Integer.parseInt(cadenceString);
		}
		catch(NumberFormatException nfe) {
			LOGGER.warning(String.format("%s.constructor: Cadence must be an integer (%s)",CLSS,nfe.getLocalizedMessage()));
		}
		LOGGER.info(String.format("%s: started with cadence %d msecs",CLSS,cadence));
	}
	
	/**
	 * The server creates controllers for the Terminal and Command sockets, and a controller
	 * for repeating requests (on a timer). The motor group controller has its own model and is already
	 * instantiated at this point..
	 */
	public void createControllers() {
		Map<String, Integer> sockets = model.getSockets();
		Iterator<String>walker = sockets.keySet().iterator();
		while( walker.hasNext()) {
			String key = walker.next();
			HandlerType type = HandlerType.valueOf(model.getHandlerTypes().get(key));
			int port = sockets.get(key);
			if( type.equals(HandlerType.COMMAND)) {
				commandController = new SocketController(this,type.name(),port); 
				commandController.addChangeListener(this);
				LOGGER.info(String.format("%s: created command controller",CLSS));
			}
			else if( type.equals(HandlerType.TERMINAL)) {
				terminalController = new SocketController(this,type.name(),port); 
				terminalController.addChangeListener(this);
				LOGGER.info(String.format("%s: created terminal controller",CLSS));
			}
		}
		timerController = new TimerController(this,cadence);
	}
	
	/**
	 * Loop forever processing whatever arrives from the various controllers. Each request is
	 * handled atomically. There is no interleaving. 
	 */
	@Override
	public void execute() {
		initialize();
		start();

		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(this)));
		
		try {
			for(;;) {
				lock.lock();
				try{
					busy.await();
					long startCycle = System.currentTimeMillis();
					LOGGER.info(String.format("%s: Cycle %d ...",CLSS,cycleCount));
					if( currentRequest==null ) break;             // Causes shutdown
					// Take care of any local requests first.
					if( currentRequest!=null ) {
						if( isLocalRequest(currentRequest) ) {
							// Handle terminal local request - -create response
							MessageBottle response = createResponseForLocalRequest(currentRequest);
							sendResponse(response);
						}
						else {
							// Handle motor request
							MessageBottle response  = motorGroupController.processRequest(currentRequest );
							sendResponse(response);
						}
					}
					LOGGER.info(String.format("%s: Cycle %d complete.",CLSS,cycleCount));
					cycleCount++;
					
					long endCycle = System.currentTimeMillis();
					long elapsed = (endCycle-startCycle);
					this.cycleTime = exponentiallyWeightedMovingAverage(this.cycleTime,elapsed);
					this.dutyCycle = exponentiallyWeightedMovingAverage(this.dutyCycle,(double)elapsed/cadence);
				}
				catch(InterruptedException ie ) {}
				finally {
					lock.unlock();
				}
			}
		} 
		catch (Exception ex) {
			ex.printStackTrace();
		} 
		finally {
			stop();
		}
		System.exit(0);
	}
	
	
	public void initialize() {
		if(motorGroupController!=null ) {
			LOGGER.info(String.format("%s.execute: initializing motorGroupController",CLSS));
			motorGroupController.initialize();
		}
	}
	@Override
	public void start() {
		if(commandController!=null )  commandController.start();
		if(terminalController!=null ) terminalController.start();
		if(timerController!=null )    timerController.start();
		if(motorGroupController!=null ) {
			LOGGER.info(String.format("%s.execute: starting motorGroupController",CLSS));
			motorGroupController.start();
		}
		LOGGER.info(String.format("%s.execute: startup complete.",CLSS));
	}
	@Override
	public void stop() {
		if(commandController!=null )    commandController.stop();
		if(terminalController!=null )   terminalController.stop();
		if(timerController!=null )      timerController.stop();
		if(motorGroupController!=null ) motorGroupController.stop();
	}
	
	
	/**
	 * We've gotten a request. It may have come from either a pipe
	 * or the timer.
	 */
	@Override
	public synchronized void handleRequest(MessageBottle request) {
		currentRequest = request;
		busy.signal();
	}
	
	/**
	 * None of our controllers use this. The MotorGroupController
	 * replies directly.
	 */
	@Override
	public void handleResponse(MessageBottle response) {
		currentRequest  = null;
		LOGGER.info(String.format("%s.handleResponse: Unexpected response, shutting down",CLSS));
		busy.signal();
	}
	
	
	
	// The "local" response is simply the original request with some text
	// to send directly to the user.
	private MessageBottle createResponseForLocalRequest(MessageBottle request) {
		if( request.fetchRequestType().equals(RequestType.GET_METRIC)) {
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
				case CYCLECOUNT:
					text = "I've processed "+(int)this.cycleCount+" requests";
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
		else if(request.fetchRequestType().equals(RequestType.COMMAND)) {
			String command = request.getProperty(BottleConstants.COMMAND_NAME, "NONE");
			if( command.equalsIgnoreCase(BottleConstants.COMMAND_SHUTDOWN)) {
				System.exit(0);     // Rely on ShutdownHandler cleanup connections
			}
			else {
				String msg = String.format("Unrecognized command: %s",command);
				LOGGER.warning(String.format("%s.createResponseForLocalRequest: %s",CLSS,msg));
				request.assignError(msg);
			}
		}
		return request;
	}

	private double exponentiallyWeightedMovingAverage(double currentValue,double previousValue) {
		double result = (1.-WEIGHT)*currentValue + WEIGHT*previousValue;
		return result;
	}
	
	private boolean isLocalRequest(MessageBottle request) {
		if( request.fetchRequestType().equals(RequestType.GET_METRIC)) {
			return true;
		}
		else if( request.fetchRequestType().equals(RequestType.COMMAND)) {
			return true;
		}
		return false;
	}
	
	// Inform both controllers that we've started ...
	private void reportStartup(String sourceName) {
		MessageBottle startMessage = new MessageBottle();
		startMessage.assignRequestType(RequestType.NOTIFICATION);
		startMessage.setProperty(BottleConstants.TEXT, "Bert is ready");
		LOGGER.info(String.format("%s: Bert is ready ... (to %s)",CLSS,sourceName));
		startMessage.assignSource(sourceName);
		sendResponse(startMessage);
	}
	
	// Return response to the request source.
	private void sendResponse(MessageBottle response) {
		String source = response.fetchSource();
		if( source.equalsIgnoreCase(HandlerType.COMMAND.name()) ) {	
			commandController.receiveResponse(response);
		}
		else if( source.equalsIgnoreCase(HandlerType.TERMINAL.name()) ) {
			terminalController.receiveResponse(response);
		}
		else {
			LOGGER.warning(String.format("%s.sendResponse: Unknown destination - %s, ignored",CLSS,source));
		}
	}
	
	
	// =============================== SocketStateChangeListener ============================================
	@Override
	public void stateChanged(SocketStateChangeEvent event) {
		if( event.getState().equals(SocketStateChangeEvent.READY)) {
			reportStartup(event.getName());
		}
	}
	
	
	
	// ==================================== Main =================================================
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
			System.out.println(USAGE);
			System.exit(1);
		}


		// Analyze command-line argument to obtain the robot root directory.
		String arg = args[0];
		Path path = Paths.get(arg);
		PathConstants.setHome(path);
		// Setup logging to use only a file appender to our logging directory
		LoggerUtility.getInstance().configureRootLogger(LOG_ROOT);
		
		RobotMotorModel mmodel = new RobotMotorModel(PathConstants.CONFIG_PATH);
		mmodel.populate();    // Analyze the xml for motor groups
		MotorGroupController mgc = new MotorGroupController(mmodel);
		
		RobotDispatcherModel model = new RobotDispatcherModel(PathConstants.CONFIG_PATH);
		model.populate();    // Analyze the xml
		Server runner = new Server(model,mgc);
		runner.createControllers();
		runner.execute();

  
	}



}
