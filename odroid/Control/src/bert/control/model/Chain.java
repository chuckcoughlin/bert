package bert.control.model;

import java.util.HashMap;
import java.util.Map;

import bert.share.control.Limb;
import bert.share.motor.Joint;

/**
 * A Chain represents a tree of Links starting with the 
 * origin link.
 */
public class Chain {
	private final static String CLSS = "Chain";
	private Link root;
	private final Map<String,Link> linksByAppendage;
	private final Map<Joint,Link> linksByJoint;
	private final Map<Limb,Link> linksByLimb;
	
	public Chain() {
		this.root = null;
		this.linksByAppendage = new HashMap<>();
		this.linksByJoint = new HashMap<>();
		this.linksByLimb = new HashMap<>();
	}
	
	/**
	 * The new link gets added to the various maps that
	 * allow us to navigate the chain.
	 * @param link the new limb.
	 */
	public void addElement( Link link) {
		linksByLimb.put(link.getName(), link);
	}

	public Link getRoot() { return root; }
	public void setRoot(Link r) { this.root = r; }
	
}

