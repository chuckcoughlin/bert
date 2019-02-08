/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.command.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import bert.share.controller.Controller;
import bert.share.message.HandlerType;
import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;
import bert.speech.process.MessageTranslator;
import bert.speech.process.StatementParser;


/**
 * The Bluetooth controller handles input/output to/from an Android tablet via
 * a Bluetooth netwark. The tablet handles speech-to-text and text-to-speech. 
 */
public class BluetoothController implements Controller {
	private final static String CLSS = "BluetoothController";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private final MessageHandler dispatcher;
	private Thread runner = null;
	private final StatementParser parser;
	private final MessageTranslator translator;
	/**
	 * Constructor:
	 * @param launcher the parent application
	 * @param text prompt displayed for user entry
	 */
	public BluetoothController(MessageHandler launcher) {
		this.dispatcher = launcher;
		this.parser = new StatementParser();
		this.translator = new MessageTranslator();
	}
	
	@Override
	public void start() {
		BluetoothReader rdr = new BluetoothReader(this.dispatcher);
		runner = new Thread(rdr);
		runner.start();
	}
	
	@Override
	public void stop() {
		if( runner!=null) {
			runner.interrupt();
			runner = null;
		}
	}
	
	@Override
	public void receiveRequest(MessageBottle request ) {
		
	}
	/**
	 * The response is expected to carry understandable text, an error
	 * message or a value that can be formatted into understandable text.
	 * @param response
	 */
	@Override
	public void receiveResponse(MessageBottle response) {
		String text = translator.messsageToText(response);
		System.out.println(text);
	}

	/**
	 * Loop forever reading from the stdin. Use ANTLR to convert text into requests.
	 * Forward requests to the Terminal dispatcher.
	 */
	public class BluetoothReader implements Runnable {
		private MessageHandler dispatcher;
		
		
		public BluetoothReader(MessageHandler disp) {
			this.dispatcher = disp;
		}

		/**
		 * Forever ...
		 *   1) Read response from named pipe
		 *   2) Invoke callback method on launcher.
		 */
		public void run() {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(System.in));
				
				while (!Thread.currentThread().isInterrupted()) {
					String input = br.readLine();

					if( "q".equalsIgnoreCase(input)    ||
						"quit".equalsIgnoreCase(input) ||
						"exit".equalsIgnoreCase(input)    ) {
						dispatcher.handleRequest(null);
						break;
					}
					else if(input.isBlank()) continue;
					/*
					 * 1) Analyze the input string via ANTLR
					 * 2) Send the resulting RequestBottle to the TerminalController
					 *    The request may hang while the controller input buffer is full. 
					 * 3) On callback from the controller, convert ResponseBottle to english string
					 * 4) Send string to stdout
					 */
					else {
						MessageBottle request = parser.parseStatement(input);
						request.assignSource(HandlerType.TERMINAL.name());
						dispatcher.handleRequest(request);
					}
				}
			} 
			catch (IOException ioe) {
				ioe.printStackTrace();
			} 
			catch (Exception ex) {
				ex.printStackTrace();
			} 
			finally {
				if (br != null) {
					try {
						br.close();
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
