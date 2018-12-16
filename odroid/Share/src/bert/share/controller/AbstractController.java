/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.share.controller;

import bert.share.model.NamedPipePair;

/**
 *  A command controller is a client-side command analyzer. It receives requests
 *  via a bounded buffer and posts responses the same way
 */
public abstract class AbstractController implements Controller  {
	protected static final String CLSS = "CommandController";
	protected NamedPipePair pipe = null;



	public AbstractController() {

	}
	public void setPipe(NamedPipePair p) {this.pipe = p; }
}
