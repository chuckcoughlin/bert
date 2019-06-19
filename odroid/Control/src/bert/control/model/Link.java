package bert.control.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.complex.Quaternion;

import bert.share.control.Appendage;
import bert.share.control.Limb;
import bert.share.motor.Joint;

/**
 * In the normal case a Link is a solid connection between joints.
 * The joints are always "revolutes", that is rotational only. There
 * is no translation. The joints are always at 0 or 90 degrees to the
 * link. 
 * 
 * The "root" link has a source "joint" that can be rotated arbitrarily
 * around any of the three axes of the reference frame. The angles are
 * given by the IMU output.
 * 
 * Links with no destination joint are called "end effectors" and can
 * be expected to have one or more "appendages" for which 3D locations
 * can be calculated.
 */
public class Link {
	private final static String CLSS = "Link";
	private final Limb limb;
	private Quaternion q;    // Transform          - quaternion
	private Link parent;
	private final List<Link> children;
	private final Map<Appendage,QHolder> appendages;
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
		this.appendages = new HashMap<>();
		this.joints = new HashMap<>();
	}
	
	public Limb getName() { return this.limb; }
	public void addAppendage(Appendage a,QHolder q) { appendages.put(a,q); }
	public void addChild(Link child) { this.children.add(child); }
	public void invalidate() {
		q = null;
	}
	public void setOrigin(Quaternion origin) { this.q = origin; }
	public Link getParent() { return this.parent; }
	public void setParent(Link p) { this.parent = p; }
}
