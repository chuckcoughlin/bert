/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.model;

import java.nio.file.Path;
import java.util.logging.Logger;

import bert.share.model.AbstractRobotModel;
import bert.share.motor.MotorConfiguration;

/**
 *  This model is used exclusively for testing.
 */
public class TestRobotModel extends AbstractRobotModel  {
	private static final String CLSS = "TestRobotModel";
	private static final Logger LOGGER = Logger.getLogger(CLSS);

	
	public TestRobotModel(Path configPath) {
		super(configPath);
	}
    

	/**
	 *  No need for controllers in our test 
	 */
	public void analyzeControllers() {}
	
	/**
	 *  Analyze the document. The information retained is dependent on the context
	 *  (client or server). This must be called before the model is accessed.
	 */
	public void populate() {
		analyzeProperties();
		analyzeMotors();
		initializeMotorConfigurations();
	}
	
	/**
	 * Set the initial positions of the motors to "attention"!
	 */
	private void initializeMotorConfigurations() {
		for( MotorConfiguration mc:getMotors().values()) {
			// Set some reasonable values from the "home" pose.
			switch(mc.getName()) {
				case ABS_X:				mc.setPosition(180.); break;
				case ABS_Y:				mc.setPosition(180.); break;
				case ABS_Z:				mc.setPosition(0.); break;
				case BUST_X:			mc.setPosition(180.); break;
				case BUST_Y:			mc.setPosition(180.); break;
				case HEAD_Y:			mc.setPosition(0.); break;
				case HEAD_Z:			mc.setPosition(0.); break;
				case LEFT_ANKLE_Y:		mc.setPosition(90.); break;
				case LEFT_ARM_Z:		mc.setPosition(0.); break;
				case LEFT_ELBOW_Y:		mc.setPosition(180.); break;
				case LEFT_HIP_X:		mc.setPosition(180.); break;
				case LEFT_HIP_Y:		mc.setPosition(180.); break;
				case LEFT_HIP_Z:		mc.setPosition(0.); break;
				case LEFT_KNEE_Y:		mc.setPosition(180.); break;
				case LEFT_SHOULDER_X:	mc.setPosition(180.); break;
				case LEFT_SHOULDER_Y:	mc.setPosition(180.); break;
				case RIGHT_ANKLE_Y:		mc.setPosition(90.); break;
				case RIGHT_ARM_Z:		mc.setPosition(0.); break;
				case RIGHT_ELBOW_Y:		mc.setPosition(180.); break;
				case RIGHT_HIP_X:		mc.setPosition(180.); break;
				case RIGHT_HIP_Y:		mc.setPosition(180.); break;
				case RIGHT_HIP_Z:		mc.setPosition(0.); break;
				case RIGHT_KNEE_Y:		mc.setPosition(180.); break;
				case RIGHT_SHOULDER_X:	mc.setPosition(180.); break;
				case RIGHT_SHOULDER_Y:	mc.setPosition(180.); break;
				case UNKNOWN:			mc.setPosition(0.); break;
			}
		}
	}
}


