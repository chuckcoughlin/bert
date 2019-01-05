/**
 * Copyright 2018 ILS Automation. All rights reserved.
 */
package bert.share.bottle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import bert.share.motor.MotorConfiguration;
import bert.share.motor.MotorPosition;

/**
 * This class holds a request to the server posted by the client. As it is passed
 * across the named pipe, it is serialized into JSON.
 */
public class RequestBottle implements Serializable{
	private static final String CLSS = "RequestBottle";
	private static final long serialVersionUID = -8230038990523790456L;
	private RequestType command;
	private Properties properties;
	private List<MotorPosition> targets;
	private List<MotorConfiguration> motors;
	
	public RequestBottle() {
		this.properties = new Properties();
		this.targets = new ArrayList<>();
		this.motors  = new ArrayList<>();
	}
	
	public RequestBottle(RequestType cmd) {
		this.command = cmd;
		this.properties = new Properties();
	}

	public RequestType getCommand() { return this.command; }
	public void setCommand(RequestType cmd) { this.command = cmd; }
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
