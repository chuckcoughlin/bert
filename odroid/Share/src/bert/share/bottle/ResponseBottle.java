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
 * This class holds a response from the server to the client. As it is passed
 * across the named pipe, it is serialized into JSON.
 */
public class ResponseBottle implements Serializable, Cloneable {
	private static final String CLSS = "ResponseBottle";
	private static final long serialVersionUID = 2311476737631983034L;
	private String status;
	private Properties properties;
	private List<MotorPosition> positions;
	private List<MotorConfiguration> motors;
	
	public ResponseBottle() {
		this.properties = new Properties();
		this.positions = new ArrayList<>();
		this.motors  = new ArrayList<>();
	}
	
	public ResponseBottle(String s) {
		this.status = s;
		this.properties = new Properties();
	}

	public String getStatus() { return this.status; }
	
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
