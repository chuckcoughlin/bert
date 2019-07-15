package bert.control.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import bert.control.main.Solver;
import bert.share.common.PathConstants;
import bert.share.control.Appendage;
import bert.share.control.Limb;
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
	private Link root;
	private final Map<Appendage,Link> linkByAppendage;
	private final Map<Joint,List<Link>> linkListByJoint;
	private final Map<String,Link> linksByLimb;
	private double[] origin = new double[] { 0., 0., 0.};
	private double [] axis  = new double[] { 0., 0., 0.};
	
	public Chain() {
		this.root = null;
		this.linkByAppendage = new HashMap<>();
		this.linkListByJoint = new HashMap<>();
		this.linksByLimb = new HashMap<>();
	}
	
	/**
	 * The new link gets added to the various maps that
	 * allow us to navigate the chain.
	 * @param link the new limb.
	 */
	public void addElement( Link link) {
		linksByLimb.put(link.getName(), link);
		LinkPoint lp = link.getEndPoint();
		if( lp.getType().equals(LinkPointType.APPENDAGE)) {
			linkByAppendage.put(lp.getAppendage(), link);
		}
		else if( lp.getType().equals(LinkPointType.REVOLUTE)) {
			Joint j = lp.getJoint();
			List list = linkListByJoint.get(j);
			if(list==null ) {
				list = new ArrayList<>();
				linkListByJoint.put(j,list);
			}
			list.add(link);
		}
	}
	/**
	 * As we add origin and endpoints, the new link gets added to the various
	 * maps that allow us to navigate the chain.
	 * @param name the new link or limb name.
	 */
	public void createLink(String name) {
		Link link = new Link(name.toUpperCase());
		linksByLimb.put(link.getName(), link);
	}
	public void setOriginLinkPoint(Limb link,LinkPoint lp) {
		
	}

	public void setEndLinkPoint(Limb link,LinkPoint lp) {
		
	}

	public Collection<Link> getLinks() { return linksByLimb.values(); }
	public Link getLinkForLimb(Limb limb) { return linksByLimb.get(limb); }

	/**
	 * Traverse the entire chain, clearing temporary calculations in each link.
	 * @return
	 */
	public void invalidate() {
		for(Link link:linksByLimb.values()) {
			link.invalidate();
		}
	}
	/**
	 * Work back toward the source until we find a valid/up-to-date link.
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
    }
}

