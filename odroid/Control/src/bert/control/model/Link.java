package bert.control.model;

import org.hipparchus.complex.Quaternion;

/**
 * A Link is a solid member between two "LinkPoints" called the "origin"
 * and "endpoint". Multiple links may be connected to the same source.
 * The joints are always "revolutes", that is rotational only. There
 * is no translation. The joints are always at 0 or 90 degrees to the
 * link. 
 * 
 * Links with no destination joint are called "end effectors" and can
 * be expected to have one or more "appendages" for which 3D locations
 * can be calculated.
 * 
 * Positions of joints and appendages are always relative to the "root"
 * link's origin. Any correction due to IMU readings are handled externally.
 * 
 */
public class Link {
	private final static String CLSS = "Link";
	private final String name;
	private LinkPoint origin;
	private LinkPoint endpoint;
	private Quaternion q;    // Transform          - quaternion
	private Link parent;



	/**
	 * Define a link given the name. The name must be unique.
	 * @param name either a limb or appendage name
	 */
	public Link(String nam) {
		this.name = nam;
		this.parent=null;
		this.origin=null;
		this.endpoint = null;

	}
	
	public String getName() { return this.name; }

	public void invalidate() {
		q = null;
	}
	public LinkPoint getEndPoint() { return this.endpoint; }
	public void setEndPoint(LinkPoint end) { this.endpoint = end; }
	public LinkPoint getOrigin() { return this.origin; }
	public void setOrigin(LinkPoint o) { this.origin = o; }
	public Link getParent() { return this.parent; }
	public void setParent(Link p) { this.parent = p; }
}
