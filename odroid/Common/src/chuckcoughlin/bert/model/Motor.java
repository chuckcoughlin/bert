/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package chuckcoughlin.bert.model;

import chuckcoughlin.bert.common.DynamixelType;
import chuckcoughlin.bert.common.Joint;

/**
 * A motor refers to a Dynamixel stepper motor at one of the joints
 * of the robot. Each motor belongs to a single controller.
 */
public class Motor  {
	private static final String CLSS = "Motor";
	private final Joint name;
	private final DynamixelType type;
	private final int id;
	private final double offset;
	private final double minAngle;
	private final double maxAngle;
	private final boolean direct;
	/** 
	 * Constructor: Sets fixed attributes.
	 * @param name of the motor
	 * @param type Dynamixel model
	 * @param isDirect true if orientation is "forward"
	 */
	public Motor(Joint name,DynamixelType type,int id,double offset,double minAngle,double maxAngle,boolean isDirect) {
		this.name = name;
		this.type = type;
		this.id = id;
		this.offset = offset;
		this.minAngle = minAngle;
		this.maxAngle = maxAngle;
		this.direct = isDirect;
		
	}
	public Joint getName()         { return this.name; }
	public DynamixelType getType() { return this.type; }
	public int getId()             { return this.id; }
	public double getOffset()      { return this.offset; }
	public double getMinAngle()    { return this.minAngle; }
	public double getMaxAngle()    { return this.maxAngle; }
	public boolean isDirect()      { return this.direct; }
	
}
