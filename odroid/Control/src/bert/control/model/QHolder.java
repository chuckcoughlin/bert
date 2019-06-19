package bert.control.model;

/**
 * This class is a container for a Quaternion that describes the current
 * position of a joint and how it was derived.
 */
public class QHolder {
	private final static String CLSS = "QHolder";
	private final double[] translation;
	private double[] rotation;

	/**
	 * Holder of information for quaternions. Offsets are meters
	 * from the source point.
	 * @param xyz array of offsets in 3 dimension ~m. 
	 * @param rot rotation array in 3 dimension ~m. 
	 */
	public QHolder(double[] xyz,double[] rot) {
		this.translation = xyz;
		this.rotation = rot;
	}
	

}
