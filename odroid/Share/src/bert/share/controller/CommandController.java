/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import java.util.logging.Logger;

import bert.share.bottle.MessageBottle;
import bert.share.common.NamedPipePair;


/**
 * A command controller encapsulates a single named pipe pair. It accepts a command request
 * from the speech processor and returns a response. 
 * 
 * There are two possible read behaviors:
 *   1) synchronous mode, the caller blocks on the "getMessage" method. 
 *   2) asynchronous mode, a non-owner caller must implement a "handleResponse" callback.
 */
public class CommandController {
	private final static String CLSS = "CommandController";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private final ControllerLauncher launcher;
	private final NamedPipePair pipe;
	private final boolean synchronous;
	private Thread runner = null;

	
	/**
	 * Constructor:
	 * @param launcher
	 * @param p
	 * @param synch true if the caller will use synchronous reads. Otherwise we monitor
	 *        the response side of the pipe and return the result in a callback.
	 */
	public CommandController(ControllerLauncher launcher,NamedPipePair p,boolean synch) {
		this.launcher = launcher;
		this.pipe = p;
		this.synchronous = synch;
		pipe.setReadsAsynchronous(false);
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
		private ControllerLauncher launcher;
		private NamedPipePair pipe;
		
		
		public AsynchronousReader(ControllerLauncher l,NamedPipePair p) {
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
