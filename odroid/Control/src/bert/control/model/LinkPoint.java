package bert.control.model;

import bert.share.control.Appendage;
import bert.share.motor.Joint;
/**
 * A LinkPoint is a hinged joint (as they all are).
 * The position is a 3D location of the joint 
 * with respect to the source joint of the link.
 * 
 * The rotation array show the direction of the
 * joint along one of the major axes with respect
 * to the link.
 */
public class LinkPoint {
	private final static String CLSS = "LinkPoint";
	private final double[] axis;
	private final double[] position;
	private final LinkPointType type;
	private final Appendage appendage;
	private final Joint joint;

	public LinkPoint(Appendage app,double[] ax,double[] pos ) {
		this.type = LinkPointType.APPENDAGE;
		this.appendage = app;
		this.joint = null;
		this.position = pos;
		this.axis = ax;
	}
	
	public LinkPoint(Joint j,double[] ax,double[] pos ) {
		this.type = LinkPointType.REVOLUTE;
		this.appendage = null;
		this.joint = j;
		this.position = pos;
		this.axis = ax;
	}
	
	public LinkPoint() {
		this.type = LinkPointType.ORIGIN;
		this.appendage = null;
		this.joint = null;
		this.position = new double[] {0.,0.,0.};
		this.axis     = new double[] {0.,0.,0.};
	}
		
	
	public String getName() { return joint.name(); }
	public LinkPointType getType() { return this.type; }
	public Appendage getAppendage() { return this.appendage; }
	public double[] getAxis() { return this.axis; }
	public Joint getJoint() { return this.joint; }
	public double[] getPosition() { return this.position; }
}
