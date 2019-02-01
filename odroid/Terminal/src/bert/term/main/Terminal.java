/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.term.main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import bert.share.bottle.MessageBottle;
import bert.share.common.PathConstants;
import bert.share.controller.Dispatcher;
import bert.share.logging.LoggerUtility;
import bert.share.model.ConfigurationConstants;
import bert.share.pipe.NamedPipeController;
import bert.sql.db.Database;
import bert.term.model.RobotTerminalModel;


/**
 * "Terminal" is an application that allows interaction from the command line
 * to command and interrogate the robot. Command entries are the same as
 * those given to the "headless" application, "Bert", in spoken form.
 * 
 * The application acts as the intermediary between a StdioController and
 * NamedPipeController communicating with the Server.
 */
public class Terminal implements Dispatcher {
	private final static String CLSS = "Terminal";
	private static final String USAGE = "Usage: terminal <robot_root>";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static final String LOG_ROOT = "terminal";
	private final RobotTerminalModel model;
	private NamedPipeController pipeController = null;
	private final Condition busy;
	private StdioController controller = null;
	private MessageBottle currentRequest;
	private final Lock lock;
	
	public Terminal(RobotTerminalModel m) {
		this.model = m;
		this.lock = new ReentrantLock();
		this.busy = lock.newCondition();
	}

	/**
	 * This application contains a stdio controller and a pipe controller
	 */
	@Override
	public void createControllers() {
		String prompt = model.getProperty(ConfigurationConstants.PROPERTY_PROMPT,"bert:");
		this.controller = new StdioController(this,prompt);
		
		Map<String, String> pipeNames = model.getPipeNames();
		Iterator<String>walker = pipeNames.keySet().iterator();
		String key = walker.next();
		String pipeName = pipeNames.get(key);
		this.pipeController = new NamedPipeController(this,pipeName,false); 
	}
	
	/**
	 * Loop forever reading from the terminal and forwarding the resulting requests
	 * via named pipe to the server. We accept responses and forward to the stdio
	 * controller.
	 */
	@Override
	public void execute() {
		initialize();
		start();
		try {
			for(;;) {
				lock.lock();
				try{
					busy.await();
					if( currentRequest==null) break;
					pipeController.receiveRequest(currentRequest);	
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
		Database.getInstance().shutdown();
		System.exit(0);
	}

	@Override
	public void initialize() {
		pipeController.initialize();
		controller.initialize();
	}
	@Override
	public void start() {
		pipeController.start();
		controller.start();
	}
	@Override
	public void stop() {
		pipeController.stop();
		controller.stop();
	}
	/**
	 * We've gotten a request (must be from a different thread than our main loop). Signal
	 * to release the lock and send along to the pipe.
	 */
	@Override
	public synchronized void handleRequest(MessageBottle request) {
		currentRequest = request;
		busy.signal();
	}
	
	/**
	 * We've gotten a response. Send it to our Stdio controller 
	 * which ultimately writes it to stdout.
	 */
	@Override
	public void handleResponse(MessageBottle response) {
		controller.receiveResponse(response);
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
		Database.getInstance().populateMotors(model.getMotors());
		Terminal runner = new Terminal(model);
		runner.createControllers();
        runner.execute();
	}

}
