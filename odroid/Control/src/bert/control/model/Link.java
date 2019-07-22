package bert.control.model;

import java.util.logging.Logger;

import org.hipparchus.complex.Quaternion;

/**
 * A Link is a solid member between two "LinkPoints" called the "origin"
 * and "linkPoint". Multiple links may be connected to the same source.
 * The joints are always "revolutes", that is rotational only. There
 * is no translation. The joint axis is always at 0 or 90 degrees to a
 * line from the origin to linkPoint. 
 * 
 * Links with no destination joint are called "end effectors" and can
 * be expected to have one or more "appendages" for which 3D locations
 * can be calculated.
 * 
 * Within the linkPoint object, coordinates are with respect to the
 * link's origin and are static. Coordinates within the link object
 * temporary and are calculated with respect to the inertial frame of 
 * reference. Any corrections due to IMU readings are handled externally.
 * 
 */
public class Link {
	private final static String CLSS = "Link";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private final String name;
	private LinkPoint linkpoint;
	private boolean dirty = true;  // Requires calculations
	private Link parent;
	private double angle;
	private double[] coordinates = { 0.,0.,0.};


	/**
	 * Define a link given the name. The name must be unique.
	 * @param name either a limb or appendage name
	 */
	public Link(String nam) {
		this.name = nam;
		this.parent=null;
		this.linkpoint = null;
		this.angle = 0.;
	}
	
	public String getName() { return this.name; }
	public boolean isDirty() { return this.dirty; }
	/**
	 * Mark link as needing new calculations. We do this because sub-chains
	 * that share links can avoid redundant computations. 
	 */
	public void setDirty() { this.dirty = true; }
	/**
	 * The joint angle is the motor position. Set it in degrees, read it in radians.
	 * Note that changing the angle does not invalidate the current link, just its children.
	 * @return
	 */
	public double getJointAngle() { return this.angle; }
	public void setJointAngle(double a) { this.angle=a*Math.PI/180.; }    // Convert to radians
	public LinkPoint getLinkPoint() { return this.linkpoint; }
	public void setEndPoint(LinkPoint end) { this.linkpoint = end; }
	public LinkPoint getOrigin() { 
		if( parent==null) return LinkPoint.getOrigin();
		else return parent.getLinkPoint();
	}
	public Link getParent() { return this.parent; }
	public void setParent(Link p) { this.parent = p; }
	
	/**
	 * Return the coordinates of the endpoint relative to inertial frame. The endpoint is
	 * either a joint or appendage.
	 * 
	 * 1) Get the parent's rotation, add the orientation of the origin. This represents
	 *    the rotation angle in the inertial frame.
	 * 2) Take the local offset, use quaternion multiplication to compute the local
	 *    relative position of the joint in intertial frame.
	 * 3) Add this position to the parent coordinates resulting in the absolute position
	 *    of the joint with respect to the inertial frame.
	 *    
	 * The coordinates are returned immediately if the link is "clean", otherwise the
	 * above calculations are required, possibly up the chain to the root node.
	 *  
	 * @return the coordinates of the joint/appendage associated with this link in meters
	 *         with respect to the inertial frame of reference. 
	 */
	public double[] getCoordinates() {
		double[] pos = null;   // Coordinates in progress
		double[] rotation = null;
		double alpha = 0.;
		if( dirty ) {
			if( parent!=null) {
				pos    = parent.getCoordinates();
				alpha  = parent.getJointAngle();
				double[] orient = parent.getLinkPoint().getOrientation();
				rotation = rotationFromCoordinates(pos);
				rotation[0] = rotation[0]+orient[0];
				rotation[1] = rotation[1]+orient[1];
				rotation[2] = rotation[2]+orient[2];
			}
			else {
				pos = LinkPoint.getOrigin().getOffset();
				rotation = LinkPoint.getOrigin().getOrientation();
			}
			LOGGER.info(String.format("%s.getCoordinates: %s (%s) ---------------",CLSS,name,linkpoint.getJoint().name()));
			LOGGER.info(String.format("           position = %.2f,%.2f,%.2f",pos[0],pos[1],pos[2]));
			LOGGER.info(String.format("           rotation = %.2f,%.2f,%.2f",rotation[0],rotation[1],rotation[2]));
			double[] offset = linkpoint.getOffset();
			LOGGER.info(String.format("           offset   = %.2f,%.2f,%.2f",offset[0],offset[1],offset[2]));
			Quaternion q0 = new Quaternion(alpha,rotation[0],rotation[1],rotation[2]);
			LOGGER.info(String.format("           q0       = %.2f,%.2f,%.2f,%.2f",q0.getQ0(),q0.getQ1(),q0.getQ2(),q0.getQ3()));
			Quaternion v  = new Quaternion(0.,offset[0],offset[1],offset[2]);
			LOGGER.info(String.format("           v        = %.2f,%.2f,%.2f,%.2f",v.getQ0(),v.getQ1(),v.getQ2(),v.getQ3()));
			Quaternion inverse = q0.getInverse();
			LOGGER.info(String.format("           inverse  = %.2f,%.2f,%.2f,%.2f",inverse.getQ0(),inverse.getQ1(),inverse.getQ2(),inverse.getQ3()));
			Quaternion result = q0.multiply(v).multiply(inverse);
			LOGGER.info(String.format("           result   = %.2f,%.2f,%.2f,%.2f",result.getQ0(),result.getQ1(),result.getQ2(),result.getQ3()));
			
			coordinates[0] = pos[0] + result.getScalarPart()*result.getQ1();
			coordinates[1] = pos[1] + result.getScalarPart()*result.getQ2();
			coordinates[2] = pos[2] + result.getScalarPart()*result.getQ3();
			LOGGER.info(String.format("      coordinates   = %.2f,%.2f,%.2f",coordinates[0],coordinates[1],coordinates[2]));
			dirty = false;
		}
		return coordinates;
	}
	
	
	private double[] rotationFromCoordinates(double[] cc) {
		double len = Math.sqrt(cc[0]*cc[0] + cc[1]*cc[1] + cc[2]*cc[2]);
		if(len==0.) len = 1.0;   // All angles will be 90 deg
		double[] rot = new double[3];
		rot[0] = Math.acos(cc[0]/len);
		rot[0] = Math.acos(cc[1]/len);
		rot[0] = Math.acos(cc[2]/len);
		return rot;
	}

}
