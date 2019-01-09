/**
 * Copyright 2018 ILS Automation. All rights reserved.
 */
package bert.share.bottle;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import bert.share.motor.MotorConfiguration;
import bert.share.motor.MotorPosition;

/**
 * This is the parent class of requests and responses that are passed
 * across named pipes, it is serialized into JSON.
 */
public class BasicBottle {
	private static final String CLSS = "BasicBottle";
	protected Properties properties;
	protected List<MotorPosition> targets;
	protected List<MotorConfiguration> motors;
	
	public BasicBottle() {
		this.properties = new Properties();
		this.targets = new ArrayList<>();
		this.motors  = new ArrayList<>();
	}

	public String getProperty(String key,String defaultValue) {
		return this.properties.getProperty(key, defaultValue);
	}
	public void setProperty(String key,String value) {
		this.properties.setProperty(key, value);
	}
	/**
	 * This is a convenience method retrieve an error message property.
	 * The message should be suitable for direct playback to the user.
	 * 
	 * @return an error message. If there is no error message, return null.
	 */
	public String getError() {
		return this.properties.getProperty(BottleConstants.ERROR__MESSAGE);
	}
	
	/**
	 * This is a convenience method to set a string suitable for audio
	 * that indicates an error. If an error is present no further
	 * processing is valid.
	 * 
	 * @param msg a message suitable to be played for the user.
	 */
	public void setError(String msg) {
		this.properties.setProperty(BottleConstants.ERROR__MESSAGE, msg);
	}
	
}
