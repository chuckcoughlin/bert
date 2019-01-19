/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.dispatcher.main;

import bert.share.bottle.MessageBottle;
import bert.share.common.NamedPipePair;

/**
 *  A dispatch controller is a server-side controller for the receiving end of a Command
 *  or Terminal pipe. 
 */
public class DispatchController  {
	protected static final String CLSS = "DispatchController";
	private System.Logger LOGGER = System.getLogger(CLSS);
	private NamedPipePair pipe = null;
	
	/**
	 * Constructor:
	 * @param p
	 */
	public DispatchController(NamedPipePair p) {
		this.pipe = p;
		pipe.setReadsAsynchronous(true);
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
	}
	
	public void stop() {
		pipe.shutdown();
	}
}
