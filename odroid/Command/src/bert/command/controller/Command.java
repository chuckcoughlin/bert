/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.command.controller;

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

import bert.command.model.RobotCommandModel;
import bert.share.common.BottleConstants;
import bert.share.common.PathConstants;
import bert.share.controller.SocketController;
import bert.share.message.HandlerType;
import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;
import bert.share.message.MetricType;
import bert.share.message.RequestType;
import bert.share.model.Appendage;
import bert.share.model.ConfigurationConstants;
import bert.share.model.Joint;
import bert.share.util.LoggerUtility;
import bert.share.util.ShutdownHook;
import bert.speech.process.MessageTranslator;
import bert.sql.db.Database;

/**
 * This is the main client class that handles spoken commands and forwards
 * them on to the central launcher. It also handles database actions 
 * involving playback and record.
 */
public class Command extends Thread implements MessageHandler {
	private final static String CLSS = "Command";
	private static final String USAGE = "Usage: command <config-file>";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static final long EXIT_WAIT_INTERVAL = 1000;
	private static final String LOG_ROOT = CLSS.toLowerCase();
	private final RobotCommandModel model;
	private BluetoothController tabletController = null;
	private final MessageTranslator messageTranslator;
	private SocketController dispatchController = null;
	private final Condition busy;
	private MessageBottle currentRequest;
	private final Lock lock;
	private boolean ignoring;
	
	
	public Command(RobotCommandModel m) {
		this.model = m;
		this.lock = new ReentrantLock();
		this.busy = lock.newCondition();
		this.ignoring = false;
		this.messageTranslator = new MessageTranslator();
	}

	/**
	 * This application routes requests/responses between the Dispatcher and "blueserverd" daemon. Both
	 * destinations involve socket controllers.
	 */
	@Override
	public void createControllers() {
		this.tabletController = new BluetoothController(this,model.getBlueserverPort());
		String hostName = model.getProperty(ConfigurationConstants.PROPERTY_HOSTNAME, "localhost");
		Map<String, Integer> sockets = model.getSockets();
		Iterator<String>walker = sockets.keySet().iterator();
		String key = walker.next();
		int port = sockets.get(key);
		this.dispatchController = new SocketController(this,HandlerType.COMMAND.name(),hostName,port); 
	}
	
	@Override
	public String getControllerName() { return model.getProperty(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, "command"); }
	
	/**
	 * Loop forever reading from the bluetooth daemon (representing the tablet) and forwarding the resulting requests
	 * via socket to the server (launcher). We accept its responses and forward back to the tablet.
	 * Communication with the tablet consists of simple strings, plus a 4-character header.
	 */
	@Override
	public void run() {
		try {
			for(;;) {
				lock.lock();
				try{
					busy.await();
					if( currentRequest==null) break;
					if( currentRequest.fetchRequestType().equals(RequestType.COMMAND)        &&
						BottleConstants.COMMAND_HALT.equalsIgnoreCase(currentRequest.getProperties().get(BottleConstants.COMMAND_NAME)) ) {
							dispatchController.receiveRequest(currentRequest);  // halt the dispatcher as well
							Thread.sleep(EXIT_WAIT_INTERVAL);
							break;
					}
					else if( isLocalRequest(currentRequest) ) {
						// Handle local request -create response
						MessageBottle response = handleLocalRequest(currentRequest);
						if(response!=null) handleResponse(response);
					}
					else if(!ignoring) {
						dispatchController.receiveRequest(currentRequest);
					}
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
		Database.getInstance().shutdown();
		System.exit(0);
	}
	

	@Override
	public void startup() {
		dispatchController.start();
		tabletController.start();
	}
	@Override
	public void shutdown() {
		dispatchController.stop();
		tabletController.stop();
	}
	/**
	 * We've gotten a request (presumably from the BluetoothController). Signal
	 * to release the lock to send along to the dispatcher.
	 */
	@Override
	public void handleRequest(MessageBottle request) {
		lock.lock();
		try {
			currentRequest = request;
			busy.signal();
		}
		finally {
			lock.unlock();
		}
	}
	/**
	 * We've gotten a response. Send it to our BluetoothController 
	 * which ultimately writes it to the Android tablet.
	 */
	@Override
	public void handleResponse(MessageBottle response) {
		tabletController.receiveResponse(response);
	}

	// We handle the command to sleep and awake immediately.
	private MessageBottle handleLocalRequest(MessageBottle request) {
		if(request.fetchRequestType().equals(RequestType.COMMAND)) {
			String command = request.getProperty(BottleConstants.COMMAND_NAME, "NONE");
			LOGGER.warning(String.format("%s.handleLocalRequest: command=%s",CLSS,command));
			if( command.equalsIgnoreCase(BottleConstants.COMMAND_SLEEP)) {
				ignoring = true;  
			}
			else if( command.equalsIgnoreCase(BottleConstants.COMMAND_WAKE)) {
				ignoring = false;
			}
			else {
				String msg = String.format("I don't recognize command %s",command);
				request.assignError(msg);
			}
		}
		request.assignText(messageTranslator.randomAcknowledgement());
		return request;
	}
		
	// Local requests are those that can be handled immediately without forwarding to the dispatcher.
	private boolean isLocalRequest(MessageBottle request) {
		if (request.fetchRequestType().equals(RequestType.COMMAND) ) {
			Map<String,String> properties = request.getProperties();
			String cmd = properties.get(BottleConstants.COMMAND_NAME);
			if( cmd.equalsIgnoreCase(BottleConstants.COMMAND_SLEEP) ||
				cmd.equalsIgnoreCase(BottleConstants.COMMAND_WAKE) ) {
				return true;
			}
		}
		return false;
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
			System.out.println(USAGE);
			System.exit(1);
		}
		
		// Analyze command-line argument to obtain the configuration file path.
		String arg = args[0];
		Path path = Paths.get(arg);
		PathConstants.setHome(path);
		// Setup logging to use only a file appender to our logging directory
		LoggerUtility.getInstance().configureRootLogger(LOG_ROOT);
		
		RobotCommandModel model = new RobotCommandModel(PathConstants.CONFIG_PATH);
		model.populate();
		Database.getInstance().startup(PathConstants.DB_PATH);
		
        Command runner = new Command(model);
        runner.createControllers();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(runner));
        runner.startup();
        runner.start();
	}

}
