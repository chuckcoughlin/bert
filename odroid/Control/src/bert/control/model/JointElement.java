package bert.control.model;

import bert.share.motor.Joint;
/**
 * A JointElement is a hinged joint.
 */
public class JointElement implements ChainElement {
	private final static String CLSS = "JointElement";
	
	private final Link joint;

	public JointElement(Link j) {
		this.joint = j;
	}
		
	
	public String getName() { return joint.name(); }
}
