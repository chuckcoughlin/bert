/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.term.main;

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
 * The StdIO controller handles input/output to/from stdin and sdout for interactive
 * command-line operation. The typed commands and text responses are exactly the same
 * as the spoken interface with the Command application. 
 */
public class StdioController implements Controller {
	private final static String CLSS = "StdioController";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private final MessageHandler dispatcher;
	private Thread runner = null;
	private final StatementParser parser;
	private final MessageTranslator translator;
	private final String prompt;
	/**
	 * Constructor:
	 * @param launcher the parent application
	 * @param text prompt displayed for user entry
	 */
	public StdioController(MessageHandler launcher,String text) {
		this.dispatcher = launcher;
		this.prompt = text;
		this.parser = new StatementParser();
		this.translator = new MessageTranslator();
	}
	
	@Override
	public void initialize() {	
	}
	
	@Override
	public void start() {
		StdinReader rdr = new StdinReader(this.dispatcher);
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
	public class StdinReader implements Runnable {
		private MessageHandler dispatcher;
		
		
		public StdinReader(MessageHandler disp) {
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
					System.out.print(prompt);
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
						request.setSource(HandlerType.TERMINAL.name());
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
