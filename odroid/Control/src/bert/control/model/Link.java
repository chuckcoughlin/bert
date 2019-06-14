package bert.control.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.complex.Quaternion;

import bert.share.control.Limb;
import bert.share.motor.Joint;

/**
 * A Link is a solid connection between two joints.
 * A link that is an "origin" or "end-point" connects to
 * only one joint.
 */
public class Link {
	private final static String CLSS = "Link";
	private final Limb limb;
	private Quaternion q0;    // Origin          - quaternion
	private Link parent;
	private final List<Link> children;
	private final Map<Joint,QHolder> joints;

	/**
	 * 
	 * @param lnk
	 * @param p parent. If null this is the origin.
	 */
	public Link(Limb lnk) {
		this.limb = lnk;
		this.parent=null;
		this.children = new ArrayList<>();
		this.joints = new HashMap<>();
	}
	
	public Limb getName() { return this.limb; }
	public void addChild(Link child) { this.children.add(child); }
	public void setOrigin(Quaternion origin) { this.q0 = origin; }
	public Link getParent() { return this.parent; }
	public void setParent(Link p) { this.parent = p; }
}
