/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * MIT License
 */
package bert.share.bottle;

import java.io.IOException;
import java.io.Serializable;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import bert.share.motor.MotorConfiguration;
import bert.share.motor.MotorPosition;

/**
 * This class holds the requests and responses that are sent
 * across the named pipes. it is serialized into JSON.
 * 
 * There is no intrinsic difference between requests and
 * responses. We leave it to context to determine which is which.
 */
public class MessageBottle implements Serializable {
	private static final String CLSS = "MessageBottle";
	private static final System.Logger LOGGER = System.getLogger(CLSS);
	protected Properties properties;
	protected List<MotorPosition> targets;
	protected List<MotorConfiguration> motors;
	
	public MessageBottle() {
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
	 * Use this method to clear the message properties. This conveniently
	 * allows re-use of the message object when issuing a new command.
	 */
	public void clear() {
		this.properties.clear();
	}
	/**
	 * This is a convenience method retrieve an error message property.
	 * The message should be suitable for direct playback to the user.
	 * 
	 * @return an error message. If there is no error message, return null.
	 */
	public String getError() {
		return this.properties.getProperty(BottleConstants.PROPERTY_ERROR);
	}
	
	/**
	 * This is a convenience method to set a string suitable for audio
	 * that indicates an error. If an error is present no further
	 * processing is valid.
	 * 
	 * @param msg a message suitable to be played for the user.
	 */
	public void setError(String msg) {
		this.properties.setProperty(BottleConstants.PROPERTY_ERROR, msg);
	}
	
	/**
	 * For a message that is a request, the request type should be set using
	 * the setter supplied in this class.
	 * 
	 * @return the RequestType. If not set, return NONE.
	 */
	public RequestType getRequestType() {
		RequestType type = RequestType.NONE;
		String prop = this.properties.getProperty(BottleConstants.PROPERTY_REQUEST);
		if( prop!=null) {
			type = RequestType.valueOf(prop);
		}
		return type;
	}
	
	/**
	 * For a message that is a request, use this method to set its type. This
	 * is our way of enforcing a fixed vocabulary.
	 * 
	 * @param type the type of request.
	 */
	public void setRequestType(RequestType type) {
		this.properties.setProperty(BottleConstants.PROPERTY_REQUEST, type.name());
	}
	/**
	 * For a message that is a response, the type should be set using
	 * the setter supplied in this class.
	 * 
	 * @return the ResponseType. If not set, return NONE.
	 */
	public ResponseType getResponseType() {
		ResponseType type = ResponseType.NONE;
		String prop = this.properties.getProperty(BottleConstants.PROPERTY_REQUEST);
		if( prop!=null) {
			type = ResponseType.valueOf(prop);
		}
		return type;
	}
	
	/**
	 * For a message that is a response, use this method to set its type. This
	 * is our way of enforcing a fixed vocabulary.
	 * 
	 * @param type the type of request.
	 */
	public void setResponseType(ResponseType type) {
		this.properties.setProperty(BottleConstants.PROPERTY_RESPONSE, type.name());
	}
	
	// =================================== JSON ======================================
	public static MessageBottle fromJSON(String json) {
		MessageBottle bottle = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			bottle = mapper.readValue(json,MessageBottle.class);
			
		}
		catch(JsonParseException jpe) {
			LOGGER.log(Level.ERROR,String.format("%s.fromJSON: Parse exception (%s) from %s",CLSS,jpe.getLocalizedMessage(),json));
		}
		catch(JsonMappingException jme) {
			LOGGER.log(Level.ERROR,String.format("%s.fromJSON: Mapping exception (%s) from %s",CLSS,jme.getLocalizedMessage(),json));
		}
		catch(IOException ioe) {
			LOGGER.log(Level.ERROR,String.format("%s.fromJSON: IO exception (%s)",CLSS,ioe.getLocalizedMessage()));
		}
		
		return bottle;
	}
	
	public String toJSON() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		String json="";
		try {
			json = mapper.writeValueAsString(this);
		}
		catch(Exception ex) {
			LOGGER.log(Level.ERROR,String.format("%s.toJSON: Exception (%s)",CLSS,ex.getLocalizedMessage()));
		}
		return json;
	}
}
