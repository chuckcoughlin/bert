/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.command.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import bert.share.controller.Controller;
import bert.share.controller.SocketStateChangeEvent;
import bert.share.controller.SocketStateChangeListener;
import bert.share.message.HandlerType;
import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;
import bert.speech.process.MessageTranslator;
import bert.speech.process.StatementParser;


/**
 * The Bluetooth controller handles input/output to/from an Android tablet via
 * a Bluetooth network. The tablet handles speech-to-text and text-to-speech. 
 */
public class BluetoothController implements Controller {
	private final static String CLSS = "BluetoothController";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private final MessageHandler dispatcher;
	private final StatementParser parser;
	private final MessageTranslator translator;
	private final BluetoothSocket socket;
	private Thread runner = null;
	private final List<SocketStateChangeListener> changeListeners = new ArrayList<>();
	/**
	 * Constructor: For this connection, we act as a server.
	 * @param launcher the parent application
	 * @param mac address of the tablet device
	 * @param uuid of the service on that device
	 */
	public BluetoothController(MessageHandler launcher,String mac,String uuid) {
		this.dispatcher = launcher;
		this.parser = new StatementParser();
		this.translator = new MessageTranslator();
		this.socket = new BluetoothSocket(mac,uuid);
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
	 * Format a request from the spoken text arriving from the tablet.
	 * Forward on to the dispatcher.
	 * 
	 */
	@Override
	public void receiveRequest(MessageBottle request ) {
		dispatcher.handleRequest(request);
	}
	/**
	 * Extract text from the message and forward on to the tablet. The text
	 * is expected to be understandable, an error message or a value that can
	 * be formatted into plain English.
	 *
	 * @param response
	 */
	@Override
	public void receiveResponse(MessageBottle response) {
		String text = translator.messsageToText(response);
		socket.write(text);
	}
	
	
	// ===================================== Background Reader ==================================================
	/**
	 * Perform a blocking read as a background thread.
	 */
	public class BackgroundReader implements Runnable {
		private BluetoothSocket sock;


		public BackgroundReader(BluetoothSocket s) {
			this.sock = s;
		}

		/**
		 * Forever ...
		 *   1) Read request from socket
		 *   2) Invoke callback method on dispatcher
		 */
		public void run() {
			sock.discover();
			sock.startup();
			notifyChangeListeners(sock.getName(),SocketStateChangeEvent.READY);

			try {
				while(!Thread.currentThread().isInterrupted() ) {
					String input = sock.read();
					MessageBottle request = parser.parseStatement(input);
					request.assignSource(HandlerType.COMMAND.name());
					if( request.fetchError()==null) {
						receiveRequest(request);
					}
					else {
						receiveResponse(request);  // Handle error immediately
					}	
				}
			} 
			catch (IOException ioe) {
				ioe.printStackTrace();
			} 
			catch (Exception ex) {
				ex.printStackTrace();
			} 

			LOGGER.info(String.format("BackgroundReader,%s stopped",sock.getName()));
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
	
}
