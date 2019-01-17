/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.dispatcher.main;

import bert.share.bottle.MessageBottle;
import bert.share.common.NamedPipePair;

/**
 *  A command handler is a server-side controller for the receiving end of a Command
 *  or Terminal pipe. On receipt of a request, it posts the ...
 */
public class CommandHandler  {
	protected static final String CLSS = "CommandHandler";
	private final Dispatcher dispatcher;
	private NamedPipePair pipe = null;

	public CommandHandler(Dispatcher server) {
		this.dispatcher = server;
	}

	public MessageBottle getLocalRequest() {
		return null;
	}
	
	public void setPipe(NamedPipePair p) { this.pipe = p; }
}
