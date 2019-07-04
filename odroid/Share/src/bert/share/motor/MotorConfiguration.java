/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.share.motor;

import java.io.Serializable;
import java.util.logging.Logger;

import bert.share.common.DynamixelType;

/**
 * A motor refers to a Dynamixel stepper motor at one of the joints
 * of the robot. Each motor is in a group belonging to a single controller.
 * 
 * The parameters here are configured in EEPROM. Some may be modifiable 
 * at runtime.
 */
public class MotorConfiguration implements Serializable  {
	private static final String CLSS = "MotorConfiguration";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private static final long serialVersionUID = -3452548869138158183L;
	private Joint name;
	private DynamixelType type = DynamixelType.MX28;
	private int id;
	private String controller;
	private double offset;    // Configured position correction
	private double minAngle;
	private double maxAngle;
	private boolean direct;
	// Save the current goal (or actual) values. All other members
	private double position;  // ~ degrees
	private double speed;     // ~ degrees/second
	private double torque;	  // ~ N-m
	private long travelTime; // ~msecs
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
		this.direct = true;
		this.travelTime = 0;
		// These are current goal settings
		this.position = 0.;     // Pure guess
		this.speed  = 684.;     // Power-off AX-12
		this.torque = 0.;       // Power-off value
	} 
	
	/** 
	 * Constructor: Sets configuration attributes. The rest are left at default jointValues.
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
	public long getTravelTime()	   { return this.travelTime; }
	public boolean isDirect()      { return this.direct; }
	// These are the current values
	public double getPosition()    { return this.position; }
	public double getSpeed()       { return this.speed; }
	public double getTorque()      { return this.torque; }
	
	public void setController(String cntrl)  { this.controller = cntrl; }
	public void setId(int identifier)        { this.id = identifier; }
	public void setIsDirect(boolean flag)    { this.direct = flag; }
	public void setMinAngle(double angle)    { this.minAngle = angle; }
	public void setMaxAngle(double angle)    { this.maxAngle = angle; }
	public void setName(Joint jname)         { this.name = jname; }
	public void	setOffset(double off)        { this.offset = off; }
	public void setType(String typ)          { this.type = DynamixelType.valueOf(typ.toUpperCase()); }
	
	public void setProperty(String propertyName, double value) {
		try {
			JointProperty jp = JointProperty.valueOf(propertyName.toUpperCase());
			setProperty(jp,value);
		}
		catch(IllegalArgumentException iae) {
			LOGGER.warning(String.format("%s.setProperty: Illegal property %s (%s)",CLSS,propertyName,iae.getLocalizedMessage()));
		}
	}
	/**
	 * Use the motor value to set the corresponding property.
	 * @param jp JointProperty that we are setting
	 * @param value the new value
	 */
	public void setProperty(JointProperty jp, double value) {
		switch(jp) {
			case POSITION: setPosition(value); break;
			case SPEED:	   setSpeed(value);    break;
			case TORQUE:   setTorque(value);   break;
			default: break; // Ignore
		}
	}
	/**
	 * When we set a new position, use the previous position and speed
	 * to estimate the travel time.
	 * @param p
	 */
	public void setPosition(double p) { 
		double delta = this.position - p;
		if( delta<0. ) delta = -delta;
		if( speed>0. ) this.travelTime = (long)(1000.*delta/speed);  // ~msecs
		this.position = p; 
	}
	public void setSpeed(double s)    	     { this.speed = s; }
	public void setTorque(double t)          { this.torque = t; }
	
}
