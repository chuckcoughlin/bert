/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.share.motor;

import java.io.Serializable;

import bert.share.common.DynamixelType;
import bert.share.common.Joint;

/**
 * A motor refers to a Dynamixel stepper motor at one of the joints
 * of the robot. Each motor is in a group belonging to a single controller.
 * 
 * The parameters here are configured in EEPROM. Some may be modifiable 
 * at runtime.
 */
public class MotorConfiguration implements Serializable  {
	private static final String CLSS = "MotorConfiguration";
	private static final long serialVersionUID = -3452548869138158183L;
	private Joint name;
	private DynamixelType type = DynamixelType.MX28;
	private int id;
	private String controller;
	private double offset;
	private double minAngle;
	private double maxAngle;
	private double speed;
	private double torque;
	private boolean direct;
	
	/**
	 * Default constructor, necessary for serialization. Initialize all members.
	 */
	public MotorConfiguration() {
		this.name = Joint.UNKNOWN;
		this.type = DynamixelType.MX28;
		this.id = 0;
		this.controller = "";
		this.offset = 0.;
		this.minAngle = -90.;
		this.maxAngle = 90.;
		this.speed = 0.5;     // As fraction of max
		this.torque = 0.5;    // As fraction of max
		this.direct = true;
	} 
	
	/** 
	 * Constructor: Sets fixed attributes. The rest are left at default values.
	 * @param name of the motor
	 * @param type Dynamixel model
	 * @param isDirect true if orientation is "forward"
	 */
	public MotorConfiguration(Joint name,DynamixelType type,int id,String cntrl,boolean isDirect) {
		super();
		this.name = name;
		this.type = type;
		this.id = id;
		this.controller = cntrl;
		this.direct = isDirect;
		
	}
	public Joint getName()         { return this.name; }
	public DynamixelType getType() { return this.type; }
	public int getId()             { return this.id; }
	public String getController()  { return this.controller; }
	public double getOffset()      { return this.offset; }
	public double getMinAngle()    { return this.minAngle; }
	public double getMaxAngle()    { return this.maxAngle; }
	public double getSpeed()       { return this.speed; }
	public double getTorque()      { return this.torque; }
	public boolean isDirect()      { return this.direct; }
	
	public void setController(String cntrl)  { this.controller = cntrl; }
	public void setId(int identifier)        { this.id = identifier; }
	public void setIsDirect(boolean flag)    { this.direct = flag; }
	public void setMinAngle(double angle)    { this.minAngle = angle; }
	public void setMaxAngle(double angle)    { this.maxAngle = angle; }
	public void setName(String nam)          { this.name = Joint.valueOf(nam.toUpperCase()); }
	public void	setOffset(double off)        { this.offset = off; }
	public void setSpeed(double s)    	     { this.speed = s; }
	public void setTorque(double t)          { this.torque = t; }
	public void setType(String typ)          { this.type = DynamixelType.valueOf(typ.toUpperCase()); }
	
}
