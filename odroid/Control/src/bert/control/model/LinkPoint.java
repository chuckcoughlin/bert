package bert.control.model;

import bert.share.model.Appendage;
import bert.share.model.Joint;
/**
 * A LinkPoint is a hinged joint (as they all are).
 * The coordinates are a 3D location of the joint 
 * with respect to the origin of the link.
 * 
 * The orientation array shows the direction of the
 * axis of the joint with respect a line from the joint 
 * to the link origin. The offset coordinates are with respect
 * to the link origin. In most cases, the linkPoint
 * is along the z axis.
 */
public class LinkPoint {
	private final static String CLSS = "LinkPoint";
	private static LinkPoint origin = null;
	private double[] orientation;
	private double[] offset;  // Joint offset
	private final LinkPointType type;
	private final Appendage appendage;
	private final Joint joint;

	public LinkPoint(Appendage app,double[] rot,double[] pos ) {
		this.type = LinkPointType.APPENDAGE;
		this.appendage = app;
		this.joint = null;
		this.offset = pos;
		this.orientation = degreesToRadians(rot);
	}
	
	public LinkPoint(Joint j,double[] rot,double[] pos ) {
		this.type = LinkPointType.REVOLUTE;
		this.appendage = null;
		this.joint = j;
		this.offset = pos;
		this.orientation = degreesToRadians(rot);
	}
	
	/**
	 * Special constructor for the origin.
	 */
	public LinkPoint() {
		this.type = LinkPointType.ORIGIN;
		this.appendage = null;
		this.joint = Joint.UNKNOWN;
		this.offset = new double[] {0.,0.,0.};
		this.orientation   = new double[] {0.,0.,0.};
	}
		
	
	public String getName() { return joint.name(); }
	public LinkPointType getType() { return this.type; }
	public Appendage getAppendage() { return this.appendage; }
	public double[] getOrientation() { return this.orientation; }
	public void setOrientation(double[] ax ) { this.orientation = ax; }
	public Joint getJoint() { return this.joint; }
	public double[] getOffset() { return this.offset; }
	
	private double[] degreesToRadians(double[] array) {
		if( array!=null ) {
			int i = 0;
			while( i<array.length ) {
				array[i] = array[i]*Math.PI/180.;
				i++;
			}
		}
		return array;
	}
	/**
	 * Create a LinkPoint representing the origin of the link chain.
	 * @return the origin
	 */
	public static LinkPoint getOrigin() {
		if( origin==null ) origin = new LinkPoint();
		return origin;
	}
}
