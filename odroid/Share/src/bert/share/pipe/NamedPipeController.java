/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.pipe;

import java.util.logging.Logger;

import bert.share.bottle.MessageBottle;
import bert.share.controller.Controller;
import bert.share.controller.Dispatcher;


/**
 * The StdIO controller handles input/output to/from stdin and sdout for interactive
 * command-line operation. The typed commands and text responses are exactly the same
 * as the spoken interface with the Command application. 
 * 
 * There are two possible read behaviors:
 *   1) synchronous mode, the caller blocks on the "getMessage" method. 
 *   2) asynchronous mode, a non-owner caller must implement a "handleResponse" callback.
 */
public class NamedPipeController implements Controller{
	private final static String CLSS = "NamedPipeController";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private final BidirectionalNamedPipe bidirectionalPipe;
	private final Dispatcher dispatcher;
	private final String name;
	private Thread runner = null;
	private final boolean server;

	
	/**
	 * Constructor:
	 * @param launcher the parent application
	 * @param pipename the core name of the pipes (two will be created)
	 * @param isServer true if the launcher the Server application and will initially 
	 *        read from the pipe.
	 */
	public NamedPipeController(Dispatcher launcher,String pipeName,boolean isServer) {
		this.dispatcher = launcher;
		this.name = pipeName;
		this.server = isServer;
		bidirectionalPipe = new BidirectionalNamedPipe(name,isServer); 
	}
	
	@Override
	public void initialize() {
		bidirectionalPipe.create();
	}
	@Override
	public void start() {
		bidirectionalPipe.startup();
		BackgroundReader rdr = new BackgroundReader(bidirectionalPipe);
		runner = new Thread(rdr);
		runner.start();
	}
	@Override
	public void stop() {
		bidirectionalPipe.shutdown();
		if( runner!=null) {
			runner.interrupt();
			runner = null;
		}
	}
	
	/**
	 * If the parent is a server, then we get the request from the pipe,
	 * else it comes from the parent directly.
	 * @param request
	 */
	@Override
	public void receiveRequest(MessageBottle request ) {
		if( server ) {
			dispatcher.handleRequest(request);
		}
		else {
			bidirectionalPipe.write(request);
		}
	}
	
	/**
	 * If the parent is a server, then it supplies the response directly.
	 * Otherwise it comes from the pipe.
	 * @param response
	 */
	@Override
	public void receiveResponse(MessageBottle response ) {
		if( server ) {
			bidirectionalPipe.write(response);
		}
		else {
			dispatcher.handleResponse(response);
		}
	}

	/**
	 * Perform a blocking read as a background thread.
	 */
	public class BackgroundReader implements Runnable {
		private BidirectionalNamedPipe pipe;
		
		
		public BackgroundReader(BidirectionalNamedPipe p) {
			this.pipe = p;
		}

		/**
		 * Forever ...
		 *   1) Read request/response from named pipe
		 *   2) Invoke callback method on dispatcher or
		 *      local method, as appropriate.
		 */
		public void run() {
			while(!Thread.currentThread().isInterrupted() ) {
				MessageBottle msg = pipe.read();
				if( pipe.isServer() ) {
					receiveRequest(msg);
				}
				else {
					receiveResponse(msg);
				}
			}
		}
	}
}
