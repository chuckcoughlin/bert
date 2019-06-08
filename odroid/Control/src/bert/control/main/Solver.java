/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.main;

import java.nio.file.Path;
import java.util.Map;

import bert.share.model.AbstractRobotModel;
import bert.share.motor.MotorConfiguration;

/**
 *  This class handles various computations pertaining to the robot,
 *  including: trajectory planning.
 */
public class Solver {
	private final static String CLSS = "Solver";

	/**
	 * Constructor:
	 */
	public Solver() {
	}
	
	/**
	 * Some of the joint parameters are in the robot configuration file. Use them.
	 * @param urdfPath
	 * @param model
	 */
	public void configure(Path urdfPath,AbstractRobotModel model) {
		Map<String,MotorConfiguration> mcmap = model.getMotors();
	}


}

