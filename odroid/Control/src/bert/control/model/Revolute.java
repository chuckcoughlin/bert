package bert.control.model;

import bert.share.motor.Joint;
import bert.share.control.Limb;
/**
 * A Revolute is a hinged joint (as they all are).
 */
public class Revolute {
	private final static String CLSS = "Revolute";
	private final Limb parent;
	private final Limb child;
	
	private final Joint joint;

	public Revolute(Joint j,Limb p,Limb c ) {
		this.joint = j;
		this.parent= p;
		this.child = c;
	}
		
	
	public String getName() { return joint.name(); }
	public Limb getChild() { return this.child; }
	public Limb getParent() { return this.parent; }
}
