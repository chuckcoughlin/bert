package bert.control.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import bert.control.main.Solver;
import bert.share.common.PathConstants;
import bert.share.control.Appendage;
import bert.share.logging.LoggerUtility;
import bert.share.motor.Joint;

/**
 * A Chain represents a tree of Links starting with the 
 * "root" link. The position of links within the chain are
 * all relative to the root link (i.e. origin). The URDF
 * file format doesn't define things in the most convenient
 * order.
 * 
 * Changes to the "inertial frame" as detected by the IMU
 * are all handled here.
 */
public class Chain {
	private final static String CLSS = "Chain";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private Link root;
	private final Map<Appendage,Link> linkByAppendage;
	private final Map<Joint,Link> jointParent;
	private final Map<String,Link> linksByLimbName;
	private double[] origin = new double[] { 0., 0., 0.};
	private double [] axis  = new double[] { 0., 0., 0.};
	
	public Chain() {
		this.root = null;
		this.jointParent = new HashMap<>();
		this.linkByAppendage = new HashMap<>();
		this.linksByLimbName = new HashMap<>();
	}
	
	/**
	 * As we add origin and endpoints, the new link gets added to the various
	 * maps that allow us to navigate the chain.
	 * @param name the new link or limb name.
	 */
	public void createLink(String name) {
		Link link = new Link(name.toUpperCase());
		linksByLimbName.put(link.getName(), link);
	}

	public void setEndPoint(String name,LinkPoint lp) {
		Link link = linksByLimbName.get(name);
		if( link!=null ) {
			if( lp.getType().equals(LinkPointType.APPENDAGE)) {
				linkByAppendage.put(lp.getAppendage(), link);
			}
			else if( lp.getType().equals(LinkPointType.REVOLUTE)) {
				Joint j = lp.getJoint();
				//LOGGER.info(String.format("Chain.setEndPoint: add joint %s", j.name()));
				jointParent.put(j,link);
			}
			link.setEndPoint(lp);
		}
		else {
			LOGGER.warning(String.format("%s.setEndPoint: No link %s found", CLSS,name));
		}
	}

	public Collection<Link> getLinks() { return linksByLimbName.values(); }
	public Link getLinkForLimbName(String name) { return linksByLimbName.get(name.toUpperCase()); }
	/**
	 * There may be multiple joints with the same parent, but only one parent per joint.
	 * @param jointName
	 * @return the parent link of the named joint. If not found, return null.
	 */
	public Link getParentLinkForJoint(String jointName) {
		Joint joint = Joint.valueOf(jointName.toUpperCase());
		return jointParent.get(joint); 
	}
	
	/**
	 * Work back toward the root from the specified appendage. The chain
	 * is ordered to start from the root.
	 * @param appendage
	 * @return
	 */
	public List<Link> partialChainToAppendage(Appendage appendage) {
		LinkedList<Link> partial = new LinkedList<>();
		Link link = linkByAppendage.get(appendage);
		while(link!=null) {
			partial.addFirst(link);
			link = link.getParent();
		}
		return partial;
	}
	
	/**
	 * Work back toward the root link beginning with the indicated joint.
	 * The chain starts with the root.
	 * @param joint, the source
	 * @return
	 */
	public List<Link> partialChainToJoint(Joint joint) {
		LinkedList<Link> partial = new LinkedList<>();
		Link link = jointParent.get(joint);
		while(link!=null) {
			partial.addFirst(link);
			link = link.getParent();
		}
		return partial;
	}
	public Link getRoot() { return root; }
	public void setRoot(Link r) { this.root = r; }
	/**
	 * The axes are the Euler angles in three dimensions between the robot and the reference frame.
	 * @param a three dimensional array of rotational offsets between the robot and reference frame.
	 */
	public void setAxes( double[] a) { this.axis = a; }
	/**
	 * The origin is the offset of the IMU with respect to the origin of the robot.
	 * @param o three dimensional array of offsets to the origin of the chain
	 */
	public void setOrigin( double[] o) { this.origin = o; }
	/**
	 * Connect the child link to its parent.
	 * @param childName
	 * @param parentName
	 */
	public void setParent(String childName,String parentName) { 
		Link parent = linksByLimbName.get(parentName);
		Link child = linksByLimbName.get(childName);
		if( parent!=null && child!=null ) child.setParent(parent);
	}
	
	/**
	 * Test construction of the chain of robot "limbs" based on the URDF file in 
	 * $BERT_HOME/etc on the development machine.
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
		Chain chain = solver.getModel().getChain();
		
		Link root = chain.getRoot();
        System.out.println(String.format("%s: root = %s ",CLSS,root.getName()));
        // Test the links to some appendages
        System.out.println("=========================================================================");
        List<Link> subchain = chain.partialChainToAppendage(Appendage.LEFT_EAR);
        for(Link link:subchain) {
        	 System.out.println(String.format("\t%s ",link.getName()));
        }
        System.out.println("=========================================================================");
        subchain = chain.partialChainToAppendage(Appendage.RIGHT_FINGER);
        for(Link link:subchain) {
        	 System.out.println(String.format("\t%s ",link.getName()));
        }
        System.out.println("=========================================================================");
        subchain = chain.partialChainToAppendage(Appendage.RIGHT_TOE);
        for(Link link:subchain) {
        	 System.out.println(String.format("\t%s ",link.getName()));
        }
        System.out.println("=========================================================================");
        subchain = chain.partialChainToJoint(Joint.ABS_Y);
        for(Link link:subchain) {
        	 System.out.println(String.format("\t%s ",link.getName()));
        }
    }
}

