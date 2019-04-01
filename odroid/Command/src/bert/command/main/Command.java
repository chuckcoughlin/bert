/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.command.main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import bert.command.controller.BluetoothController;
import bert.command.model.Humanoid;
import bert.command.model.RobotCommandModel;
import bert.share.common.PathConstants;
import bert.share.controller.SocketController;
import bert.share.logging.LoggerUtility;
import bert.share.message.HandlerType;
import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;
import bert.share.model.ConfigurationConstants;
import bert.share.util.ShutdownHook;
import bert.sql.db.Database;

/**
 * This is the main client class that handles spoken commands and forwards
 * them on to the central dispatcher. It also handles database actions 
 * involving playback and record.
 */
public class Command extends Thread implements MessageHandler {
	private final static String CLSS = "Command";
	private static final String USAGE = "Usage: command <config-file>";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static final String LOG_ROOT = CLSS.toLowerCase();
	private final RobotCommandModel model;
	private BluetoothController controller = null;
	private SocketController socketController = null;
	private final Humanoid robot;
	private final Condition busy;
	private MessageBottle currentRequest;
	private final Lock lock;
	
	
	public Command(RobotCommandModel m) {
		this.robot = Humanoid.getInstance();
		this.model = m;
		this.lock = new ReentrantLock();
		this.busy = lock.newCondition();
	}

	/**
	 * This application is only interested in the command controller. There should only be
	 * one entry in the map. Create a socket controller for clients.
	 */
	@Override
	public void createControllers() {
		this.controller = new BluetoothController(this,model.getDeviceMAC(),model.getDeviceUUID());
		
		String hostName = model.getProperty(ConfigurationConstants.PROPERTY_HOSTNAME, "localhost");
		Map<String, Integer> sockets = model.getSockets();
		Iterator<String>walker = sockets.keySet().iterator();
		String key = walker.next();
		int port = sockets.get(key);
		this.socketController = new SocketController(this,HandlerType.COMMAND.name(),hostName,port); 
	}
	
	@Override
	public String getControllerName() { return model.getProperty(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, "command"); }
	
	/**
	 * Loop forever reading from the bluetooth tablet. Forward the resulting requests
	 * via named pipe to the server. We accept responses and forward to the tablet.
	 */
	@Override
	public void run() {

		try {
			for(;;) {
				lock.lock();
				try{
					busy.await();
					if( currentRequest==null) break;
					socketController.receiveRequest(currentRequest);	
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
		socketController.start();
		controller.start();
	}
	@Override
	public void shutdown() {
		socketController.stop();
		controller.stop();
	}
	/**
	 * We've gotten a request (must be from a different thread than our main loop). Signal
	 * to release the lock to send along to the socket.
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
	 * We've gotten a response. Send it to our Bluetooth controller 
	 * which ultimately writes it to the Android tablet.
	 */
	@Override
	public void handleResponse(MessageBottle response) {
		controller.receiveResponse(response);
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
		Database.getInstance().populateMotors(model.getMotors());
        Command runner = new Command(model);
        runner.createControllers();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(runner));
        runner.startup();
        runner.start();
	}

}
