/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import bert.share.model.NamedPipePair;

/**
 *  A command controller is a client-side command analyzer. It receives requests
 *  via a bounded buffer and posts responses the same way
 */
public abstract class AbstractController implements Controller, Runnable  {
	protected static final String CLSS = "CommandController";
	protected NamedPipePair pipe = null;
	protected final String key;
	protected final ControllerLauncher launcher;


	/**
	 * Constructor: Provide a key that is unique to the controller instance.
	 *              This is used to identify the controller in the callback response
	 * @param key identifying string.
	 * @param app the application that launched this instance
	 */
	public AbstractController(String key,ControllerLauncher app) {
		this.key = key;
		this.launcher = app;
	}
	
	public void setPipe(NamedPipePair p) {this.pipe = p; }
	
	public abstract void run();
}
