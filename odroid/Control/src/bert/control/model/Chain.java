package bert.control.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import bert.share.control.Appendage;
import bert.share.control.Limb;
import bert.share.motor.Joint;

/**
 * A Chain represents a tree of Links starting with the 
 * origin link.
 */
public class Chain {
	private final static String CLSS = "Chain";
	private Link root;
	private final Map<Appendage,Link> linksByAppendage;
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

	/**
	 * Traverse the entire chain, clearing temporary calculations in each link.
	 * @return
	 */
	public void clear() {
		for(Link link:linksByLimb.values()) {
			link.clear();
		}
	}
	/**
	 * Work back toward the source until we find a valid/up-to-date link.
	 * @param appendage
	 * @return
	 */
	public List<Link> partialChainToAppendage(Appendage appendage) {
		LinkedList<Link> partial = new LinkedList<>();
		Link link = linksByAppendage.get(appendage);
		while(link!=null) {
			partial.addFirst(link);
			link = link.getParent();
		}
		return partial;
	}
	public Link getRoot() { return root; }
	public void setRoot(Link r) { this.root = r; }
	
}

