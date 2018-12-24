/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License. 
 */
package bert.client.main;

import java.lang.System.Logger.Level;

import bert.client.model.RobotClientModel;
import bert.share.controller.AbstractController;
import bert.share.controller.Controller;
import bert.share.controller.ControllerLauncher;
import bert.share.model.AbstractRobotModel;

/**
 *  A client controller that handles reading prior joint positions from a SQLite
 *  database and sending them to the server in a set cadence.
 */
public class PlaybackController extends AbstractController implements Controller, Runnable { 
	protected static final String CLSS = "PlaybackController";
	private System.Logger LOGGER = System.getLogger(CLSS);
	private RobotClientModel model = null;
	
	public PlaybackController(String key,ControllerLauncher launcher) {
		super(key,launcher);
	}

	@Override
	public void configure(AbstractRobotModel absModel) {
		if( absModel instanceof RobotClientModel ) {
			this.model = (RobotClientModel)absModel;
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
