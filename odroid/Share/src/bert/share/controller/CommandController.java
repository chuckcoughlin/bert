/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.share.controller;

import bert.share.util.BoundedBuffer;

/**
 *  A command controller is a client-side command analyzer. It receives requests
 *  via a bounded buffer and posts responses the same way
 */
public class CommandController extends AbstractController implements Controller  {
	protected static final String CLSS = "CommandController";

	
	private BoundedBuffer incoming;
	private BoundedBuffer outgoing;


	public CommandController() {

	}

	public BoundedBuffer getClientToControllerBuffer() { return this.incoming; }
	public BoundedBuffer getControllerToClinetBuffer() { return this.outgoing; }
}
