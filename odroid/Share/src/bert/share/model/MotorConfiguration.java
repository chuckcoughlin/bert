/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.share.model;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * A motor refers to a Dynamixel stepper motor at one of the joints
 * of the robot. Each motor is in a group belonging to a single controller.
 * Optionally it may be part of a "Limb", a group of related motors.
 * All motors in a limb are managed by the same controller.
 * 
 * The parameters here are configured in EEPROM. Some may be modifiable 
 * at runtime.
 */
public class MotorConfiguration implements Serializable  {
	private static final String CLSS = "MotorConfiguration";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private static final long serialVersionUID = -3452548869138158183L;
	private Joint joint;
	private Limb limb;
	private DynamixelType type = DynamixelType.MX28;
	private int id;
	private String controller;
	private boolean torqueEnabled;  // Torque-enable - on/off 
	private double offset;    // Configured position correction
	private double minAngle;
	private double maxAngle;
	private double maxSpeed;
	private double maxTorque;
	private boolean direct;
	// Save the current goal (or actual) values. All other members
	private double position;  // ~ degrees
	private double speed;     // ~ degrees/second
	private double temperature; // deg C
	private double torque;	  // ~ N-m
	private long travelTime; // ~msecs
	/**
	 * Default constructor, necessary for serialization. Initialize all members.
	 */
	public MotorConfiguration() {
		this.joint = Joint.UNKNOWN;
		this.limb = Limb.UNKNOWN;
		this.type = DynamixelType.MX28;
		this.id = 0;
		this.controller = "";
		this.offset = 0.;
		this.minAngle = -90.;
		this.maxAngle = 90.;
		this.maxSpeed = 600.;
		this.maxTorque= 1.9;
		this.direct = true;
		this.travelTime = 0;
		// These are current goal settings
		this.position = 0.;     // Pure guess
		this.speed  = 684.;     // Power-off AX-12
		this.temperature = 20.; // Room temperature
		this.torque = 0.;       // Power-off value
		this.torqueEnabled = true;  // Initially torque is enabled 
	} 
	
	/** 
	 * Constructor: Sets configuration attributes. The rest are left at default jointValues.
	 * @param j name of the motor (its joint)
	 * @param type Dynamixel model
	 * @param isDirect true if orientation is "forward"
	 */
	public MotorConfiguration(Joint j,DynamixelType type,int id,String cntrl,boolean isDirect) {
		super();
		this.joint = j;
		this.type = type;
		this.id = id;
		this.controller = cntrl;
		this.direct = isDirect;
	}
	// Setting torque enable is essentially powering the motor on/off
	public boolean isTorqueEnabled() { return this.torqueEnabled; }
	public Joint getJoint()        { return this.joint; }
	public Limb getLimb()          { return this.limb; }
	public DynamixelType getType() { return this.type; }
	public int getId()             { return this.id; }
	public String getController()  { return this.controller; }
	public double getOffset()      { return this.offset; }
	public double getMinAngle()    { return this.minAngle; }
	public double getMaxAngle()    { return this.maxAngle; }
	public double getMaxSpeed()    { return this.maxSpeed; }
	public double getMaxTorque()   { return this.maxTorque; }
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
	public void setMaxSpeed(double val)      { this.maxSpeed = val; }
	public void setMaxTorque(double val)     { this.maxTorque = val; }
	public void setJoint(Joint jnt)          { this.joint = jnt; }
	public void setLimb(Limb lmb)        	 { this.limb = lmb; }
	public void	setOffset(double off)        { this.offset = off; }
	public void	setTemperature(double temp)  { this.temperature = temp; }
	public void setTorqueEnabled(boolean flag)	 { this.torqueEnabled = flag; }
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
			case STATE:	   setState(value);    break;
			case TEMPERATURE: setTemperature(value);   break;
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
	public void setState(double s)    	     { int state = (int)s; setTorqueEnabled(( state!=0)); }
	public void setTorque(double t)          { this.torque = t; }
	
}