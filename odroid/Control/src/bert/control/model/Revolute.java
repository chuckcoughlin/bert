package bert.control.model;

import bert.share.motor.Joint;
/**
 * A Revolute is a hinged joint (as they all are).
 */
public class Revolute {
	private final static String CLSS = "Revolute";
	private final Link parent;
	private final Link child;
	
	private final Joint joint;

	public Revolute(Joint j,Link p,Link c ) {
		this.joint = j;
		this.parent= p;
		this.child = c;
	}
		
	
	public String getName() { return joint.name(); }
	public Link getChild() { return this.child; }
	public Link getParent() { return this.parent; }
}
