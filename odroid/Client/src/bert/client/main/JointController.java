/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License. 
 */
package bert.client.main;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import bert.client.model.RobotClientModel;
import bert.share.controller.AbstractController;
import bert.share.controller.Controller;
import bert.share.controller.ControllerLauncher;
import bert.share.model.AbstractRobotModel;
import bert.share.motor.Motor;

/**
 *  A Joint controller receives positioning commands from the server for a group
 *  of motors.
 */
public class JointController extends AbstractController implements Controller  {
	protected static final String CLSS = "JointController";
	private System.Logger LOGGER = System.getLogger(CLSS);
	private RobotClientModel model = null;
	private final List<Motor> motors;

	public JointController(String key,ControllerLauncher launcher) {
		super(key,launcher);
		this.motors = new ArrayList<>();
	}
	
	public List<Motor> getMotors() {
		return motors;
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
