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
import bert.share.controller.NamedSocket;
import bert.share.controller.SocketStateChangeEvent;
import bert.share.controller.SocketStateChangeListener;
import bert.share.message.HandlerType;
import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;
import bert.share.message.RequestType;
import bert.speech.process.MessageTranslator;
import bert.speech.process.StatementParser;


/**
 * The Bluetooth controller handles input/output to/from an Android tablet via
 * a Bluetooth network. The tablet handles speech-to-text and text-to-speech. 
 * This extends SocketController to handle translation of MessageBottle objects
 * to and from the simple text messages recognized by the tablet.
 */
public class BluetoothController extends SocketController implements Controller {
	private final static String CLSS = "BluetoothController";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private final StatementParser parser;
	private final MessageTranslator translator;
	
	/**
	 * Constructor: For this connection, we act as a client.
	 * @param launcher the parent application
	 * @param port for socket connection
	 */
	public BluetoothController(MessageHandler launcher,int port) {
		super(launcher,HandlerType.TABLET.name(),"localhost",port);
		this.parser = new StatementParser();
		this.translator = new MessageTranslator();
	}
	
	/**
	 * Format a request from the spoken text arriving from the tablet.
	 * Forward on to the dispatcher.
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
		private NamedSocket sock;


		public BackgroundReader(NamedSocket s) {
			this.sock = s;
		}

		/**
		 * Forever ...
		 *   1) Read request from socket
		 *   2) Invoke callback method on dispatcher
		 */
		public void run() {
			sock.startup();
			notifyChangeListeners(sock.getName(),SocketStateChangeEvent.READY);

			try {
				while(!Thread.currentThread().isInterrupted() ) {
					String input = sock.readLine();
					MessageBottle request = parser.parseStatement(input);
					request.assignSource(HandlerType.COMMAND.name());
					if( request.fetchError()!=null || request.fetchRequestType().equals(RequestType.NOTIFICATION)) {
						receiveResponse(request);  // Handle locally/immediately
					}
					else {
						receiveRequest(request);
						
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
	
}
