/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;


/**
 * The socket controller handles two-way communication across a NamedSocket.
 * 
 * Depending on which constructor is used, the connection can be configured
 * for either a server or client.
 */
public class SocketController implements Controller{
	private final static String CLSS = "SocketController";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private static final long CLIENT_READ_ATTEMPT_INTERVAL = 15000;  // 15 secs
	protected final NamedSocket socket;
	protected final MessageHandler dispatcher;
	protected Thread runner = null;
	private final boolean server;  // True of this is created by the server process.
	protected final List<SocketStateChangeListener> changeListeners = new ArrayList<>();

	/**
	 * Constructor: Use this constructor from the server process.
	 * @param launcher the parent application
	 * @param name the socket name
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
	
	public void addChangeListener(SocketStateChangeListener c) {changeListeners.add(c);}
	public void removeChangeListener(SocketStateChangeListener c) {changeListeners.remove(c);}
	
	@Override
	public void start() {
		BackgroundReader rdr = new BackgroundReader(socket);
		runner = new Thread(rdr);
		runner.start();
	}
	@Override
	public void stop() {
		if( runner!=null) {
			LOGGER.info(String.format("%s.stopping ... %s",CLSS,socket.getName()));
			runner.interrupt();
			runner = null;
		}
		socket.shutdown();
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

	// ===================================== Background Reader ==================================================
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
		 *   1) Read request/response from socket
		 *   2) Invoke callback method on dispatcher or
		 *      local method, as appropriate.
		 */
		public void run() {
			sock.create();
			sock.startup();
			notifyChangeListeners(sock.getName(),SocketStateChangeEvent.READY);
			
			while(!Thread.currentThread().isInterrupted() ) {
				MessageBottle msg = sock.read();
				if( msg==null ) {
					try { 
						Thread.sleep(CLIENT_READ_ATTEMPT_INTERVAL);  // A read error has happened, we don't want a hard loop
						continue;
					}
					catch(InterruptedException ignore) {}
				}
				if( sock.isServer() ) {
					receiveRequest(msg);
				}
				else {
					receiveResponse(msg);
				}
			}
			LOGGER.info(String.format("BackgroundReader,%s stopped",sock.getName()));
		}
	}
	
	// ===================================== Helper Methods ==================================================
	// Notify listeners in a separate thread
	protected void notifyChangeListeners(String name,String state) {
		SocketStateChangeEvent event = new SocketStateChangeEvent(this,name,state);
		if( changeListeners.isEmpty()) return;  // Nothing to do
		Thread thread = new Thread(new Runnable() {

		    @Override
		    public void run() {
		    	for(SocketStateChangeListener l: changeListeners) {
					//log.infof("%s.notifying ... %s of %s",TAG,l.getClass().getName(),value.toString());
					l.stateChanged(event);
				}        
		    }
		            
		});
		        
		thread.start();
	}
}
