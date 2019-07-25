/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hipparchus.complex.Quaternion;

import bert.control.model.Link;
import bert.control.model.TestRobotModel;
import bert.control.model.URDFModel;
import bert.share.common.PathConstants;
import bert.share.control.Appendage;
import bert.share.logging.LoggerUtility;
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
	private final static double[] ERROR_POSITION = {0.,0.,0.};
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
	 * @return the tree of links which describes the robot.
	 */
	public URDFModel getModel() { return this.model; }
	/**
	 * Traverse the tree, setting the current angles from the
	 * motor configurations. Mark the links as "dirty".
	 */
	public void setTreeState() {
		Collection<Link> links = model.getChain().getLinks();
		for(Link link:links) {
			link.setDirty();
			Joint joint = link.getLinkPoint().getJoint();
			MotorConfiguration mc = motorConfigurations.get(joint);
			link.setJointAngle(mc.getPosition());
		}
	}
	/**
	 * Analyze the URDF file for robot geometry. This must be called before
	 * we set a tree state.
	 * @param mc a map of MotorConfigurations
	 * @param urdfPath
	 */
	public void configure(Map<Joint,MotorConfiguration> mc,Path urdfPath) {
		this.motorConfigurations = mc;
		LOGGER.info(String.format("%s.configure: URDF file(%s)",CLSS,urdfPath.toAbsolutePath().toString()));
		model.analyzePath(urdfPath);
	}
	
	/**
	 * Return the position of a specified appendage in x,y,z coordinates in meters from the
	 * robot origin in the pelvis.
	 */
	public double[] getPosition(Appendage appendage) {
		List<Link> subchain = model.getChain().partialChainToAppendage(appendage);
		if( subchain.size()>0 ) return subchain.get(0).getCoordinates();
		else return ERROR_POSITION;
	}
	/**
	 * Return the position of a specified joint in x,y,z coordinates in meters from the
	 * robot origin in the pelvis in the inertial reference frame.
	 */
	public double[] getPosition(Joint joint) {
		List<Link> subchain = model.getChain().partialChainToJoint(joint);
		if( subchain.size()>0 ) return subchain.get(0).getCoordinates();
		else return ERROR_POSITION;
	}
	
	/**
	 * Set the position of a joint. This is primarily for testing. It does not 
	 * cause a serial write to the motor.
	 * @param joint
	 * @param pos
	 */
	public void setJointPosition(Joint joint,double pos) {
		MotorConfiguration mc = motorConfigurations.get(joint);
		mc.setPosition(pos);
	}
	
	/**
	 * Test forward kinematic calculations of various positions of the robot skeleton. 
	 * We rely on configuration file in $BERT_HOME/etc on the development machine.
	 */
	public static void main(String [] args) {
		// Analyze command-line argument to obtain the robot root directory.
		String arg = args[0];
		Path path = Paths.get(arg);
		PathConstants.setHome(path);
		// Setup logging to use only a file appender to our logging directory
		String LOG_ROOT = CLSS.toLowerCase();
		LoggerUtility.getInstance().configureTestLogger(LOG_ROOT);
		// Analyze the xml for motor configurations. Initialize the motor configurations.
		TestRobotModel model = new TestRobotModel(PathConstants.CONFIG_PATH);
		model.populate();    //
		Solver solver = new Solver();
		solver.configure(model.getMotors(),PathConstants.URDF_PATH);
		
		//solver.setJointPosition(Joint.ABS_Y,90.);
		double[] xyz = solver.getPosition(Joint.ABS_Y);   // Just to top of pelvis
        System.out.println(String.format("%s (0.2,0,.114): xyz = %.2f,%.2f,%.2f ",Joint.ABS_Y.name(),xyz[0],xyz[1],xyz[2]));
    }

}

