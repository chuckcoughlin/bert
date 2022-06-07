/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.term.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import bert.share.common.BottleConstants;
import bert.share.common.PathConstants;
import bert.share.controller.SocketController;
import bert.share.message.HandlerType;
import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;
import bert.share.message.RequestType;
import bert.share.model.ConfigurationConstants;
import bert.share.util.LoggerUtility;
import bert.share.util.ShutdownHook;
import bert.speech.process.MessageTranslator;
import bert.sql.db.Database;
import bert.term.model.RobotTerminalModel;


/**
 * "Terminal" is an application that allows interaction from the command line
 * to command and interrogate the robot. Typed entries are the same as
 * those given to the "headless" application, "Command", in spoken form.
 * 
 * The application acts as the intermediary between a StdioController and
 * SocketController communicating with the Dispatcher.
 */
public class Terminal extends Thread implements MessageHandler {
	private final static String CLSS = "Terminal";
	private static final String USAGE = "Usage: terminal <robot_root>";
	private static final long EXIT_WAIT_INTERVAL = 1000;
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static final String LOG_ROOT = CLSS.toLowerCase();
	private final RobotTerminalModel model;
	private SocketController socketController = null;
	private final Condition busy;
	private MessageBottle currentRequest = null;
	private boolean ignoring;
	private final Lock lock;
	private StdioController stdioController = null;
	private final MessageTranslator messageTranslator;
	
	public Terminal(RobotTerminalModel m) {
		this.model = m;
		this.ignoring = false;
		this.lock = new ReentrantLock();
		this.busy = lock.newCondition();
		this.messageTranslator = new MessageTranslator();
	}

	/**
	 * This application contains a stdio stdioController and a client socket stdioController
	 */
	@Override
	public void createControllers() {
		String prompt = model.getProperty(ConfigurationConstants.PROPERTY_PROMPT,"bert:");
		this.stdioController = new StdioController(this,prompt);
		
		String hostName = model.getProperty(ConfigurationConstants.PROPERTY_HOSTNAME, "localhost");
		Map<String, Integer> sockets = model.getSockets();
		Iterator<String>walker = sockets.keySet().iterator();
		String key = walker.next();
		int port = sockets.get(key);
		this.socketController = new SocketController(this,HandlerType.TERMINAL.name(),hostName,port); 
	}
	
	@Override
	public String getControllerName() { return model.getProperty(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, "terminal"); }
	
	/**
	 * Loop forever reading from the terminal and forwarding the resulting requests
	 * via socket to the server (launcher). We accept its responses and forward to the stdio stdioController.
	 * 
	 * Note that the locks guarantee that the currentRequest global cannot be modified between the signal and wait.
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
						socketController.receiveRequest(currentRequest);  // halt the dispatcher as well
						Thread.sleep(EXIT_WAIT_INTERVAL);
						break;
					}
					else if( isLocalRequest(currentRequest) ) {
						// Handle local request -create response
						MessageBottle response = handleLocalRequest(currentRequest);
						if(response!=null) handleResponse(response);
					}
					else if(!ignoring) {
						socketController.receiveRequest(currentRequest);
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
		LOGGER.warning(String.format("%s: exiting...",CLSS));
		System.exit(0);
	}
	

	@Override
	public void startup() {
		socketController.start();
		stdioController.start();
	}
	@Override
	public void shutdown() {
		socketController.stop();
		stdioController.stop();
	}
	/**
	 * We've gotten a request (must be from a different thread than our main loop). Signal
	 * to release the lock and send along to the launcher.
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
	 * We've gotten a response. Send it to our Stdio stdioController 
	 * which ultimately writes it to stdout.
	 */
	@Override
	public void handleResponse(MessageBottle response) {
		stdioController.receiveResponse(response);
	}
	
	// We handle the command to sleep and awake immediately.
	private MessageBottle handleLocalRequest(MessageBottle request) {
		if(request.fetchRequestType().equals(RequestType.COMMAND)) {
			String command = request.getProperty(BottleConstants.COMMAND_NAME, "NONE");
			//LOGGER.warning(String.format("%s.handleLocalRequest: command=%s",CLSS,command));
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
	 * Entry point for the application that allows direct user input through
	 * stdio. The argument specifies a directory that is the root of the various
	 * robot configuration, code and devices.
	 * 
	 * Usage: terminal <bert_root> 
	 * 
	 * @param args command-line arguments. Only one matters.
	 */
	public static void main(String[] args) {
			
		// Make sure there is command-line argument
		if( args.length < 1) {
			System.out.println(USAGE);
			System.exit(1);
		}

		// Analyze command-line argument to obtain the file path to BERT_HOME.
		String arg = args[0];
		Path path = Paths.get(arg);
		PathConstants.setHome(path);
		// Setup logging to use only a file appender to our logging directory
		LoggerUtility.getInstance().configureRootLogger(LOG_ROOT);
		
		RobotTerminalModel model = new RobotTerminalModel(PathConstants.CONFIG_PATH);
		model.populate();
		Database.getInstance().startup(PathConstants.DB_PATH);
		Terminal runner = new Terminal(model);
		runner.createControllers();
		Runtime.getRuntime().addShutdownHook(new ShutdownHook(runner));
        runner.startup();
        runner.start();
	}
}
