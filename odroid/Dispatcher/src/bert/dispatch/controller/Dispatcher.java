/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.dispatch.controller;

import java.io.IOException;
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

import bert.control.controller.InternalController;
import bert.control.controller.QueueName;
import bert.control.message.InternalMessage;
import bert.control.solver.Solver;
import bert.motor.controller.MotorGroupController;
import bert.share.common.BottleConstants;
import bert.share.common.PathConstants;
import bert.share.controller.SocketController;
import bert.share.controller.SocketStateChangeEvent;
import bert.share.controller.SocketStateChangeListener;
import bert.share.message.HandlerType;
import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;
import bert.share.message.MetricType;
import bert.share.message.RequestType;
import bert.share.model.Appendage;
import bert.share.model.ConfigurationConstants;
import bert.share.model.Joint;
import bert.share.model.JointProperty;
import bert.share.model.Limb;
import bert.share.model.RobotMotorModel;
import bert.share.util.LoggerUtility;
import bert.share.util.ShutdownHook;
import bert.sql.db.Database;

/**
 * The Dispatcher is its own system process. It's job is to accept requests from 
 * the command sockets, distribute them to the motor manager channels and post the results.
 * For complicated requests it may invoke the services of the "Solver" and insert 
 * internal intermediate requests.
 */
public class Dispatcher extends Thread implements MessageHandler,SocketStateChangeListener {
	private final static String CLSS = "Dispatcher";
	private static final String USAGE = "Usage: launcher <robot_root>";
	// Phrases to choose from ...
	private static final String[] mittenPhrases = {
		"My hands cut easily",
		"My hands are cold",
		"Mittens are stylish"
	};
	private static final String[] startPhrases = {
       "Bert is ready",
       "At your command",
       "I'm listening",
       "Speak your wishes"
    };
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static final String LOG_ROOT = CLSS.toLowerCase();
	private double WEIGHT = 0.5;  // weighting to give previous in EWMA
	private final RobotMotorModel model;
	private SocketController commandController = null;
	private SocketController terminalController= null;
	private InternalController internalController     = null;
	private MotorGroupController motorGroupController = null;
	private final Condition busy;
	private MessageBottle currentRequest = null;
	private final Lock lock;
	private final Solver solver;
	
	private int cadence = 1000;     // msecs
	private	int cycleCount = 0;     // messages processed
	private	double cycleTime = 0.0; // msecs,    EWMA
	private double dutyCycle = 0.0; // fraction, EWMA
	
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public Dispatcher(RobotMotorModel m,Solver s,MotorGroupController mgc) {
		this.model = m;
		this.lock = new ReentrantLock();
		this.busy = lock.newCondition();
		this.motorGroupController = mgc;
		this.solver = s;
		setName(model.getProperty(ConfigurationConstants.PROPERTY_ROBOT_NAME,"Bert"));
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
	 * 
	 * The internal controller is used for those instances where multiple or repeating messages are
	 * required for a single user request.
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
		internalController = new InternalController(this);
	}
	
	@Override
	public String getControllerName() { return model.getProperty(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, "launcher"); }
	/**
	 * On startup, initiate a short message sequence to bring the robot into a sane state.
	 */
	@Override
	public void startup() {
		if(commandController!=null )  commandController.start();
		if(terminalController!=null ) terminalController.start();
		if(internalController!=null )    internalController.start();
		if(motorGroupController!=null ) {
			LOGGER.info(String.format("%s.execute: starting motorGroupController",CLSS));
			motorGroupController.initialize();
			motorGroupController.start();

			// Set the speed to "normal" rate. Delay to all startup to complete
			InternalMessage msg = new InternalMessage(RequestType.SET_POSE,QueueName.GLOBAL);
			msg.setProperty(BottleConstants.POSE_NAME,"normal speed");
			msg.setDelay(1000);   // 1 sec delay
			internalController.receiveRequest(msg);
			// Read all the joint positions
			msg = new InternalMessage(RequestType.LIST_MOTOR_PROPERTY,QueueName.GLOBAL);
			msg.setProperty(BottleConstants.PROPERTY_NAME,JointProperty.POSITION.name()); 
			msg.setDelay(1000);   // 1 sec delay
			internalController.receiveRequest(msg);
			// Bring any joints that are outside sane limits into compliance
			msg = new InternalMessage(RequestType.INITIALIZE_JOINTS,QueueName.GLOBAL);
			msg.setDelay(2000);   // 2 sec delay
			internalController.receiveRequest(msg);
		}
		LOGGER.info(String.format("%s.execute: startup complete.",CLSS));
	}
	@Override
	public void shutdown() {
		motorGroupController.stop();
		LOGGER.info(String.format("%s.shutdown: Shutting down 1 ...",CLSS));
		if(commandController!=null )    commandController.stop();
		LOGGER.info(String.format("%s.shutdown: Shutting down 2 ...",CLSS));
		if(terminalController!=null )   terminalController.stop();
		LOGGER.info(String.format("%s.shutdown: Shutting down 3 ...",CLSS));
		if(internalController!=null )      internalController.stop();
		LOGGER.info(String.format("%s.shutdown: Shutting down 4 ...",CLSS));
		if(motorGroupController!=null ) motorGroupController.stop();
		LOGGER.info(String.format("%s.shutdown: complete.",CLSS));
	}
	/**
	 * Loop forever processing whatever arrives from the various controllers. Each request is
	 * handled atomically. There is no interleaving. 
	 */
	@Override
	public void run() {
		//LOGGER.info(String.format("%s: Starting run loop ...",CLSS));
		try {
			for(;;) {
				lock.lock();
				try{
					LOGGER.info(String.format("%s: Entering wait for cycle %d ...",CLSS,cycleCount));
					busy.await();
					long startCycle = System.currentTimeMillis();
					LOGGER.info(String.format("%s: Cycle %d ...",CLSS,cycleCount));
					if( currentRequest==null ) break;             // Causes shutdown
					if( currentRequest!=null ) {
						// "internal" requests are those that need to be queued on the internal controller
						if( isInternalRequest(currentRequest) ) {
							MessageBottle response = handleInternalRequest(currentRequest);
							handleResponse(response);
						}
						// a "local" request can be handled without involving the motor controllers
						else if( isLocalRequest(currentRequest) ) {
							// Handle local request -create response
							MessageBottle response = handleLocalRequest(currentRequest);
							handleResponse(response);
						}
						else {
							// Handle motor request. The controller forwards response here via "handleResponse".
							motorGroupController.processRequest(currentRequest );
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
			shutdown();
		}
		System.exit(0);
	}
	
	
	
	/**
	 * We've gotten a request. It may have come from either a socket
	 * or the timer. Signal the busy lock, so main loop proceeds.
	 */
	@Override
	public synchronized void handleRequest(MessageBottle request) {
		lock.lock();
		LOGGER.info(String.format("%s.handleRequest: Processing %s from %s",CLSS,request.fetchRequestType().name(),request.fetchSource()));
		try {
			currentRequest = request;
			busy.signal();
		}
		finally {
			lock.unlock();
		}
	}
	
	/**
	 * This is called by "sub-controllers" - Internal and MotorGroup. Forward the response on to
	 * the appropriate socket controller for transmission to the original source of the request.
	 * Note: Notifications are broadcast to all live controllers.
	 */
	@Override
	public void handleResponse(MessageBottle response) {
		String source = response.fetchSource();
		LOGGER.info(String.format("%s.handleResponse: Received response %s for %s",CLSS,response.fetchRequestType().name(),source));
		if( source.equalsIgnoreCase(HandlerType.COMMAND.name()) ) {	
			commandController.receiveResponse(response);
		}
		else if( source.equalsIgnoreCase(HandlerType.TERMINAL.name()) ) {
			terminalController.receiveResponse(response);
		}
		else if( source.equalsIgnoreCase(HandlerType.DISPATCHER.name()) ) {	
			commandController.receiveResponse(response);
			terminalController.receiveResponse(response);
		}
		else if( source.equalsIgnoreCase(HandlerType.INTERNAL.name()) ) {
			internalController.receiveResponse(response);
		}
		else {
			LOGGER.warning(String.format("%s.handleResponse: Unknown destination - %s, ignored",CLSS,source));
		}
	}
	// The response is simply the request. A generic acknowledgement will be relayed to the user.
	// 1) Freezing a joint requires getting the motor position first to update the internal status dictionary
	private MessageBottle handleInternalRequest(MessageBottle request) {
		// Read the current motor positions, then freeze.
		Map<String,String> properties = request.getProperties();
		// Entire robot
		if( request.fetchRequestType().equals(RequestType.COMMAND) && 
				properties.get(BottleConstants.COMMAND_NAME).equalsIgnoreCase(BottleConstants.COMMAND_FREEZE)) {
			InternalMessage msg = new InternalMessage(RequestType.LIST_MOTOR_PROPERTY,QueueName.GLOBAL);
			msg.setProperty(BottleConstants.PROPERTY_NAME,JointProperty.POSITION.name()); 
			internalController.receiveRequest(msg);
			msg = InternalMessage.clone(request,QueueName.GLOBAL);
			msg.setDelay(1000);   // 1 sec delay
			internalController.receiveRequest(msg);
		}
		// A limb
		if( request.fetchRequestType().equals(RequestType.SET_LIMB_PROPERTY) && 
				properties.get(BottleConstants.PROPERTY_NAME).equalsIgnoreCase(JointProperty.STATE.name()) &&
				properties.get(JointProperty.STATE.name()).equalsIgnoreCase(BottleConstants.ON_VALUE)) {
			InternalMessage msg = new InternalMessage(RequestType.LIST_MOTOR_PROPERTY,QueueName.GLOBAL);
			msg.setProperty(BottleConstants.PROPERTY_NAME,JointProperty.POSITION.name()); 
			msg.setProperty(BottleConstants.LIMB_NAME,request.getProperty(BottleConstants.LIMB_NAME,Limb.UNKNOWN.name()));
			internalController.receiveRequest(msg);
			msg = InternalMessage.clone(request,QueueName.GLOBAL);
			msg.setDelay(500);   // 1/2 sec delay
			internalController.receiveRequest(msg);
		}
		// Single joint
		else if( request.fetchRequestType().equals(RequestType.SET_MOTOR_PROPERTY) && 
				properties.get(BottleConstants.PROPERTY_NAME).equalsIgnoreCase(JointProperty.STATE.name()) &&
				properties.get(JointProperty.STATE.name()).equalsIgnoreCase(BottleConstants.ON_VALUE)) {
			InternalMessage msg = new InternalMessage(RequestType.GET_MOTOR_PROPERTY,QueueName.GLOBAL);
			msg.setProperty(BottleConstants.PROPERTY_NAME,JointProperty.POSITION.name());
			msg.setProperty(BottleConstants.JOINT_NAME,request.getProperty(BottleConstants.JOINT_NAME,Joint.UNKNOWN.name()));
			internalController.receiveRequest(msg);
			msg = InternalMessage.clone(request,QueueName.GLOBAL);
			msg.setDelay(250);   // 1/4 sec delay
			internalController.receiveRequest(msg);
		}
		return request;
	}


	// Create a response for a request that can be handled immediately. The response is simply the original request
	// with some text to send directly to the user. 
	private MessageBottle handleLocalRequest(MessageBottle request) {
		// The following two requests simply use the current positions of the motors, whatever they are
		if( request.fetchRequestType().equals(RequestType.GET_APPENDAGE_LOCATION)) {
			solver.setTreeState(); // Forces new calculations
			String appendageName = request.getProperty(BottleConstants.APPENDAGE_NAME, Appendage.UNKNOWN.name());
			double[] xyz = solver.getPosition(Appendage.valueOf(appendageName));
			String text = String.format("%s is located at %0.2f %0.2f %0.2f meters",appendageName.toLowerCase(), xyz[0],xyz[1],xyz[2]);
			request.assignText(text);
		}
		else if(request.fetchRequestType().equals(RequestType.GET_JOINT_LOCATION) ) {
			solver.setTreeState();
			String jointName = request.getProperty(BottleConstants.JOINT_NAME, Joint.UNKNOWN.name());
			// Choose any one of the links attached to the joint, get its parent
			Joint joint = Joint.valueOf(jointName.toUpperCase());
			double[] xyz = solver.getPosition(joint);
			String text = String.format("The center of joint %s is located at %0.2f %0.2f %0.2f meters",jointName.toLowerCase(), xyz[0],xyz[1],xyz[2]);
			request.assignText(text);
		}
		else if( request.fetchRequestType().equals(RequestType.GET_METRIC)) {
			MetricType metric = MetricType.valueOf(request.getProperty(BottleConstants.METRIC_NAME, "NAME"));
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
				case MITTENS:
					text = selectRandomText(mittenPhrases);
					break;
				case NAME:
					text = "My name is "+getName();
					break;
			}
			request.assignText(text);
		}
		else if(request.fetchRequestType().equals(RequestType.COMMAND)) {
			String command = request.getProperty(BottleConstants.COMMAND_NAME, "NONE");
			LOGGER.warning(String.format("%s.handleLocalRequest: command=%s",CLSS,command));
			if( command.equalsIgnoreCase(BottleConstants.COMMAND_HALT)) {
				System.exit(0);     // Rely on ShutdownHandler cleanup connections
			}
			// Initiate a system poweroff - supposedly the shutdown hook will be run
			else if( command.equalsIgnoreCase(BottleConstants.COMMAND_SHUTDOWN)) {
				try {
			        Runtime rt = Runtime.getRuntime();
			        rt.exec("sudo poweroff"); 
			    } 
				catch (IOException ioe) {
					LOGGER.warning(String.format("%s.createResponseForLocalRequest: Powerdown error (%s)",CLSS,ioe.getMessage())); 
			    }
			}
			else {
				String msg = String.format("Unrecognized command: %s",command);
				request.assignError(msg);
			}
		}
		else if(request.fetchRequestType().equals(RequestType.SAVE_POSE) ) {
			String poseName = request.getProperty(BottleConstants.POSE_NAME,"");
			if( !poseName.isEmpty()) {
				Database.getInstance().saveJointPositionsForPose(model.getMotors(),poseName);	
			}
			else {
				poseName = Database.getInstance().saveJointPositionsAsNewPose(model.getMotors());
				request.assignText("I saved the pose as "+poseName);
			}
		}
		return request;
	}

	private double exponentiallyWeightedMovingAverage(double currentValue,double previousValue) {
		double result = (1.-WEIGHT)*currentValue + WEIGHT*previousValue;
		return result;
	}
	
	// These are complex requests that require that several messages be created and processed
	// on the internal controller. Categories include:
	//   1) Anything that sets "torque enable" to true. This action requires that we read
	//      and save (in memory) current motor positions.
	private boolean isInternalRequest(MessageBottle request) {
		// Never send a request launched by the internal controller back to it. That would be an infinite loop
		if( request.fetchSource().equalsIgnoreCase(HandlerType.INTERNAL.name())) return false;
		
		Map<String,String> properties = request.getProperties();
		if( request.fetchRequestType().equals(RequestType.COMMAND) && 
				properties.get(BottleConstants.COMMAND_NAME).equalsIgnoreCase(BottleConstants.COMMAND_FREEZE)) {
			return true;
		}
		else if( request.fetchRequestType().equals(RequestType.SET_LIMB_PROPERTY) && 
				properties.get(BottleConstants.PROPERTY_NAME).equalsIgnoreCase(JointProperty.STATE.name()) &&
				properties.get(JointProperty.STATE.name()).equalsIgnoreCase(BottleConstants.ON_VALUE)) {
			return true;
		}
		else if( request.fetchRequestType().equals(RequestType.SET_MOTOR_PROPERTY) && 
				properties.get(BottleConstants.PROPERTY_NAME).equalsIgnoreCase(JointProperty.STATE.name()) &&
				properties.get(JointProperty.STATE.name()).equalsIgnoreCase(BottleConstants.ON_VALUE)) {
			return true;
		}
		return false;
	}
	
	private boolean isLocalRequest(MessageBottle request) {
		if( request.fetchRequestType().equals(RequestType.GET_APPENDAGE_LOCATION) || 
			request.fetchRequestType().equals(RequestType.GET_JOINT_LOCATION)   ||
			request.fetchRequestType().equals(RequestType.GET_METRIC) ||
			request.fetchRequestType().equals(RequestType.SAVE_POSE)) {
			return true;
		}
		else if (request.fetchRequestType().equals(RequestType.COMMAND) ) {
			Map<String,String> properties = request.getProperties();
			String cmd = properties.get(BottleConstants.COMMAND_NAME);
			if( cmd.equalsIgnoreCase(BottleConstants.COMMAND_FREEZE) ||
				cmd.equalsIgnoreCase(BottleConstants.COMMAND_RELAX) ) {
				return false;
			}
			else {
				return true;
			}
		}
		else if( request.fetchRequestType().equals(RequestType.NOTIFICATION)) {
			request.assignSource(HandlerType.DISPATCHER.name());  // Setup to broadcast
			return true;
		}
		return false;
	}
	
	// Inform both controllers that we've started ...
	private void reportStartup(String sourceName) {
		MessageBottle startMessage = new MessageBottle();
		startMessage.assignRequestType(RequestType.NOTIFICATION);
		startMessage.assignText(selectRandomText(startPhrases));
		LOGGER.info(String.format("%s: Bert is ready ... (to %s)",CLSS,sourceName));
		startMessage.assignSource(sourceName);
		handleResponse(startMessage);
	}
    /**
     * Select a random startup phrase from the list.
     * @return the selected phrase.
     */
    private String selectRandomText(String[] phrases) {
        double rand = Math.random();
        int index = (int)(rand*phrases.length);
        return phrases[index];
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
	 * Entry point for the launcher application that receives commands, processes
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
		
		RobotMotorModel model = new RobotMotorModel(PathConstants.CONFIG_PATH);
		model.populate();    // Analyze the xml for motors and motor groups
		Database.getInstance().startup(PathConstants.DB_PATH);
		MotorGroupController mgc = new MotorGroupController(model);
		Solver solver = new Solver();
		solver.configure(model.getMotors(),PathConstants.URDF_PATH);
		Dispatcher runner = new Dispatcher(model,solver,mgc);
		mgc.setResponseHandler(runner);
		runner.createControllers();
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(runner)));
		runner.startup();
		runner.start();
	}



}