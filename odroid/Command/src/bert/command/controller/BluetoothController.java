/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.command.controller;

import java.util.logging.Logger;

import bert.share.common.BottleConstants;
import bert.share.controller.Controller;
import bert.share.controller.NamedSocket;
import bert.share.controller.SocketController;
import bert.share.controller.SocketStateChangeEvent;
import bert.share.message.HandlerType;
import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;
import bert.share.message.RequestType;
import bert.share.message.MessageType;
import bert.speech.process.MessageTranslator;
import bert.speech.process.StatementParser;


/**
 * The Bluetooth controller handles input/output to/from an Android tablet via
 * the blueserverd daemon. The tablet handles speech-to-text and text-to-speech. 
 * This extends SocketController to handle translation of MessageBottle objects
 * to and from the simple text messages recognized by the tablet.
 */
public class BluetoothController extends SocketController implements Controller {
	private final static String CLSS = "BluetoothController";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private final StatementParser parser;
	private final MessageTranslator translator;
	private boolean suppressingErrors;
	
	/**
	 * Constructor: For this connection, we act as a client.
	 * @param launcher the parent application
	 * @param port for socket connection
	 */
	public BluetoothController(MessageHandler launcher,int port) {
		super(launcher,HandlerType.TABLET.name(),"localhost",port);
		this.parser = new StatementParser();
		this.translator = new MessageTranslator();
		this.suppressingErrors = false;
	}
	
	@Override
	public void start() {
		BluetoothBackgroundReader rdr = new BluetoothBackgroundReader(socket);
		runner = new Thread(rdr);
		runner.start();
	}
	
	/**
	 * Format a request from the spoken text arriving from the tablet.
	 * Forward on to the launcher.
	 */
	@Override
	public void receiveRequest(MessageBottle request ) {
		launcher.handleRequest(request);
	}
	/**
	 * Extract text from the message and forward on to the tablet formatted appropriately.
	 * For straight replies, the text is expected to be understandable, an error message or a value that can
	 * be formatted into plain English.
	 * 
	 * @param response
	 */
	@Override
	public void receiveResponse(MessageBottle response) {
		String text = translator.messageToText(response);
		text = text.trim();
		socket.write(String.format("%s:%s",MessageType.ANS.name(),text));
	}
	
	/**
	 * The request can be handled immediately without being sent to the
	 * dispatcher. A common scenario is a parsing error. For partial messages
	 * we simply wait until the next parse.
	 */
	private void handleImmediateResponse(MessageBottle request) {
		if( !request.fetchRequestType().equals(RequestType.PARTIAL) ) {
			if( !suppressingErrors ) receiveResponse(request);
			else {
				suppressingErrors = true;  // Suppress replies to consecutive yntax errors
				String text = translator.messageToText(request);
				LOGGER.info(String.format("%s.SuppressedErrorMessage: %s",CLSS, text));
			}
		}
	}
	// ===================================== Background Reader ==================================================
	/**
	 * Perform a blocking read as a background thread. The specified socket
	 * connects to the "blueserverd" daemon. We are receiving requests from
	 * the tablet, but have connected as if we were a client.
	 */
	public class BluetoothBackgroundReader implements Runnable {
		private NamedSocket sock;
		
		
		public BluetoothBackgroundReader(NamedSocket s) {
			this.sock = s;
		}

		/**
		 * Forever ...
		 *   1) Read request from socket
		 *   2) Analyze text and convert into MessageBottle
		 *   3) Invoke callback method on launcher
		 *   
		 *   There is only one kind of message that we recognize. Anything 
		 *   else is an error.
		 */
		public void run() {
			sock.create();
			sock.startup();
			notifyChangeListeners(sock.getName(),SocketStateChangeEvent.READY);
			
			while(!Thread.currentThread().isInterrupted() ) {
				MessageBottle msg = null;
				String text = sock.readLine();  // Strips trailing new-line
				if( text==null || text.isEmpty()  ) {
					try { 
						Thread.sleep(CLIENT_READ_ATTEMPT_INTERVAL);  // A read error has happened, we don't want a hard loop
						continue;
					}
					catch(InterruptedException ignore) {}
				}
				else if( text.length()>BottleConstants.HEADER_LENGTH ) {
					String hdr = text.substring(0,BottleConstants.HEADER_LENGTH-1);
					if( hdr.equalsIgnoreCase(MessageType.MSG.name())) {
						// Strip header then translate the rest.
						try {
							text = text.substring(BottleConstants.HEADER_LENGTH);
							LOGGER.info(String.format("%s parsing: %s",sock.getName(),text));
							msg = parser.parseStatement(text);
						}
						catch(Exception ex) {
							msg = new MessageBottle();
							msg.assignRequestType(RequestType.NOTIFICATION);
							msg.assignError(String.format("Parse failure (%s) on: %s",ex.getLocalizedMessage(),text));
						}
					}
					else if( hdr.equalsIgnoreCase(MessageType.LOG.name())) {
						LOGGER.info(String.format("%s: %s",sock.getName(),text));
						continue;
						
					}
					else {
						msg = new MessageBottle();
						msg.assignRequestType(RequestType.NOTIFICATION);
						msg.assignError(String.format("Message has an unrecognized prefix (%s)",text));
					}
				}
				else {
					msg = new MessageBottle();
					msg.assignRequestType(RequestType.NOTIFICATION);
					msg.assignError(String.format("Received a short message from the tablet (%s)",text));
				}
				
				if( msg==null ) break;  // This happens on shutdown - I don't know how
				msg.assignSource(HandlerType.COMMAND.name());
				if( msg.fetchRequestType().equals(RequestType.NOTIFICATION) ||
					msg.fetchRequestType().equals(RequestType.NONE) 		||
					msg.fetchRequestType().equals(RequestType.PARTIAL) 		||
					msg.fetchError()!=null ) 							{
					handleImmediateResponse(msg);
				}
				else {
					suppressingErrors = false;
					receiveRequest(msg);
				}
			}
			LOGGER.info(String.format("BluetoothBackgroundReader,%s stopped",sock.getName()));
		}
	}
	
}
