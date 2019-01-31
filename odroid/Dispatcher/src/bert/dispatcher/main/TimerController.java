/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.dispatcher.main;

import java.util.logging.Logger;

import bert.share.bottle.MessageBottle;
import bert.share.common.NamedPipePair;

/**
 *  A dispatch controller is a server-side controller for the receiving end of a Command
 *  or Terminal pipe. The reads return null if there is no request pending.
 */
public class TimerController  {
	protected static final String CLSS = "DispatchController";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private NamedPipePair pipe = null;
	
	/**
	 * Constructor:
	 * @param p
	 */
	public TimerController(NamedPipePair p) {
		this.pipe = p;
		pipe.setReadsAsynchronous(true);
	}
	
	/**
	 * In our usage, the message off the pipe is always a request.
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
	}
	
	public void stop() {
		pipe.shutdown();
	}
}
