/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.term.main;

import java.util.logging.Logger;

import bert.share.bottle.MessageBottle;


/**
 * The StdIO controller handles input/output to/from stdin and sdout for interactive
 * command-line operation. The typed commands and text responses are exactly the same
 * as the spoken interface with the Command application. 
 */
public class StdioController {
	private final static String CLSS = "NamedPipeController";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private final RequestHandler launcher;
	private Thread runner = null;

	/**
	 * Constructor:
	 * @param launcher
	 * @param p
	 * @param synch true if the caller will use synchronous reads. Otherwise we monitor
	 *        the response side of the pipe and return the result in a callback.
	 */
	public StdioController(RequestHandler launcher) {
		this.launcher = launcher;

	}
	
	/**
	 * This should not be called by a non-owner who expects asynchronous behavior.
	 * The message can be either a request or response depending on circumstances.
	 * @return the contents of the pipe.
	 */
	public MessageBottle getMessage() {
		return pipe.read();
	}
	
	public void sendMessage(MessageBottle bottle)  {
		pipe.write(bottle);
	}
	
	public void start() {
		pipe.startup();
		if( !synchronous ) {
			AsynchronousReader ar = new AsynchronousReader(this.launcher,this.pipe);
			runner = new Thread(ar);
			runner.start();
		}
	}
	
	public void stop() {
		if( runner!=null) {
			runner.interrupt();
			runner = null;
		}
		pipe.shutdown();
	}
	

	

	public class AsynchronousReader implements Runnable {
		private RequestHandler launcher;
		private RequestPipe pipe;
		
		
		public AsynchronousReader(RequestHandler l,RequestPipe p) {
			this.launcher = l;
			this.pipe = p;
		}

		/**
		 * Forever ...
		 *   1) Read response from named pipe
		 *   2) Invoke callback method on launcher.
		 */
		public void run() {
			while(!Thread.currentThread().isInterrupted() ) {
				MessageBottle bottle = pipe.read();
				if(bottle!=null) launcher.handleResult(bottle);	
			}
		}
	}
}
