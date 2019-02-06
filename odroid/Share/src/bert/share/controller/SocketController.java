/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import java.util.logging.Logger;

import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;


/**
 * The StdIO controller handles input/output to/from stdin and sdout for interactive
 * command-line operation. The typed commands and text responses are exactly the same
 * as the spoken interface with the Command application. 
 * 
 * There are two possible read behaviors:
 *   1) synchronous mode, the caller blocks on the "getMessage" method. 
 *   2) asynchronous mode, a non-owner caller must implement a "handleResponse" callback.
 */
public class SocketController implements Controller{
	private final static String CLSS = "NamedPipeController";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private final NamedSocket socket;
	private final MessageHandler dispatcher;
	private Thread runner = null;
	private final boolean server;  // True of this is created by the server process.


	/**
	 * Constructor: Use this constructor from the server process.
	 * @param launcher the parent application
	 * @param port communication port number
	 */
	public SocketController(MessageHandler launcher,String name, int port) {
		this.dispatcher = launcher;
		this.server = true;
		socket = new NamedSocket(name,port); 
	}
	
	/**
	 * Constructor: Use this version for processes that are clients
	 * @param launcher the parent application
	 * @param hostname of the server process.
	 * @param port communication port number
	 */
	public SocketController(MessageHandler launcher,String name,String hostname,int port) {
		this.dispatcher = launcher;
		this.server = false;
		socket = new NamedSocket(name,hostname,port); 
	}
	
	@Override
	public void initialize() {
		socket.create();
	}
	@Override
	public void start() {
		socket.startup();
		BackgroundReader rdr = new BackgroundReader(socket);
		runner = new Thread(rdr);
		runner.start();
	}
	@Override
	public void stop() {
		socket.shutdown();
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
			socket.write(request);
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
			socket.write(response);
		}
		else {
			dispatcher.handleResponse(response);
		}
	}

	/**
	 * Perform a blocking read as a background thread.
	 */
	public class BackgroundReader implements Runnable {
		private NamedSocket sock;
		
		
		public BackgroundReader(NamedSocket s) {
			this.sock = s;
		}

		/**
		 * Forever ...
		 *   1) Read request/response from named pipe
		 *   2) Invoke callback method on dispatcher or
		 *      local method, as appropriate.
		 */
		public void run() {
			while(!Thread.currentThread().isInterrupted() ) {
				MessageBottle msg = sock.read();
				if( msg==null) continue;
				if( sock.isServer() ) {
					receiveRequest(msg);
				}
				else {
					receiveResponse(msg);
				}
			}
		}
	}
}
