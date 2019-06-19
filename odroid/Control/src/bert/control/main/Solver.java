/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;

import bert.control.model.TestRobotModel;
import bert.control.model.URDFModel;
import bert.share.common.PathConstants;
import bert.share.control.Appendage;
import bert.share.motor.Joint;
import bert.share.motor.MotorConfiguration;

/**
 *  This class handles various computations pertaining to the robot,
 *  including: trajectory planning. Note that the same link object
 *  may belong to several chains.
 */
public class Solver {
	private final static String CLSS = "Solver";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private final URDFModel model;
	private Map<Joint,MotorConfiguration> motorConfigurations;

	/**
	 * Constructor:
	 */
	public Solver() {
		this.model = new URDFModel();
		this.motorConfigurations = null;
	}
	
	/**
	 * Traverse the tree, clearing all the intermediate calculations (Quaternions).
	 * This forces them to be re-calculated.
	 */
	public void invalidateTree() {
		model.getChain().invalidate();
	}
	/**
	 * Analyze the URDF file for robot geometry.
	 * @param mc a map of MotorConfigurations
	 * @param urdfPath
	 */
	public void configure(Map<Joint,MotorConfiguration> mc,Path urdfPath) {
		this.motorConfigurations = mc;
		LOGGER.info(String.format("%s.analyzePath: URDF file(%s)",CLSS,urdfPath.toAbsolutePath().toString()));
		model.analyzePath(urdfPath);
	}
	
	/**
	 * Return the position of a specified appendage in x,y,z coordinates in meters from the
	 * robot origin in the pelvis.
	 */
	public double[] getLocation(Appendage appendage) {
		double[] xyz = new double[3];
		return xyz;
	}
	/**
	 * Return the position of a specified joint in x,y,z coordinates in meters from the
	 * robot origin in the pelvis.
	 */
	public double[] getLocation(Joint joint) {
		double[] xyz = new double[3];
		return xyz;
	}
	
	/**
	 * Test calculations of various positions of the robot skeleton. We rely on configuration
	 * files being written to $BERT_HOME/etc on the development machine.
	 */
	public static void main(String [] args) {
		// Analyze command-line argument to obtain the robot root directory.
		String arg = args[0];
		Path path = Paths.get(arg);
		PathConstants.setHome(path);;
		// Analyze the xml for motor configurations
		TestRobotModel model = new TestRobotModel(PathConstants.CONFIG_PATH);
		model.populate();    //
		Solver solver = new Solver();
		solver.configure(model.getMotors(),PathConstants.URDF_PATH);
		
		double[] xyz = solver.getLocation(Joint.ABS_Y);   // Just to top of pelvis
        System.out.println(String.format("%s: xyz = %.2f,%.2f,%.2f ",Joint.ABS_Y.name(),xyz[0],xyz[1],xyz[2]));
    }

}

