package bert.control.model;

/**
 * This class is a container for a Quaternion that describes the current
 * position of a joint and how it was derived. We save all except the angle
 * of the joint. 
 */
public class QHolder {
	private final static String CLSS = "QHolder";
	private final double distance;

	/**
	 * Holder of information for quaternions. Offsets are meters
	 * from the source point.
	 * @param xyz array of offsets in 3 dimension ~m.  
	 */
	public QHolder(double[] xyz) {
	}
	/**
	 * Specify the distance between the origin and joint
	 * @param d
	 */
	public void setDistance(double d) { this.distance=d;}
	
	public Quaternion getQuaternion() {}

}
