package bert.control.model;

import bert.share.motor.Joint;
import bert.share.control.Limb;
/**
 * A Revolute is a hinged joint (as they all are).
 * The position is a 3D location of the joint 
 * with respect to the source joint of the link.
 * 
 * The rotation array show the direction of the
 * joint along one of the major axes with respect
 * to the link.
 */
public class Revolute {
	private final static String CLSS = "Revolute";
	private final double[] position;
	private final double[] rotation;
	private final Joint joint;

	public Revolute(Joint j,double[] pos,double[] rot ) {
		this.joint = j;
		this.position = pos;
		this.rotation = rot;
	}
		
	
	public String getName() { return joint.name(); }
	public double[] getPosition() { return this.position; }
}
