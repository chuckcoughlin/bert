/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.share.motor;

import java.io.Serializable;

/**
 * A motor refers to a Dynamixel stepper motor at one of the joints
 * of the robot. Each motor belongs to a single controller.
 */
public class MotorPosition implements Serializable  {

	private static final String CLSS = "MotorPosition";
	private static final long serialVersionUID = -8712438613845232362L;
	
	private int id = 0;
	private String controller;
	private int position = 0;
	
	/**
	 * Default constructor, necessary for serialization
	 */
	public MotorPosition() {
	} 
	
	/** 
	 * Constructor: Sets fixed attributes.
	 * @param name of the motor
	 * @param type Dynamixel model
	 * @param isDirect true if orientation is "forward"
	 */
	public MotorPosition(int id,String controller,int pos) {
	
		
	}
	public int getPosition()  { return this.position; }
	
}
