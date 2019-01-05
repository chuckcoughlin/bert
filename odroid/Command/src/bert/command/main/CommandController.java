/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.command.main;

import java.lang.System.Logger.Level;

import bert.command.model.RobotCommandModel;
import bert.share.controller.AbstractController;
import bert.share.controller.Controller;
import bert.share.controller.ControllerLauncher;
import bert.share.model.AbstractRobotModel;
import bert.share.util.BoundedBuffer;

/**
 *  A command controller is a client-side command analyzer. It receives requests
 *  via a bounded buffer and posts responses the same way
 */
public class CommandController extends AbstractController implements Controller, Runnable { 
	protected static final String CLSS = "CommandController";
	private System.Logger LOGGER = System.getLogger(CLSS);
	private final int BUFFER_SIZE = 5;
	private RobotCommandModel model = null;
	private final BoundedBuffer buffer;


	public CommandController(String key,ControllerLauncher launcher) {
		super(key,launcher);
		this.buffer = new BoundedBuffer(BUFFER_SIZE);
	}

	public BoundedBuffer getClientToControllerBuffer() { return this.buffer; }

	@Override
	public void configure(AbstractRobotModel absModel) {
		if( absModel instanceof RobotCommandModel ) {
			this.model = (RobotCommandModel)absModel;
		}
		else {
			LOGGER.log(Level.ERROR,String.format("%s.configure: Wrong class (%s) given to method",CLSS,absModel.getClass().getCanonicalName()));
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
