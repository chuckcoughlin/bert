/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * MIT License
 */
package bert.share.bottle;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class holds the requests and responses that are sent
 * across the named pipes. it is serialized into JSON.
 * 
 * There is no intrinsic difference between requests and
 * responses. We leave it to context to determine which is which.
 */
public class MessageBottle implements Serializable {
	private static final long serialVersionUID = 4356286171135500644L;
	private static final String CLSS = "MessageBottle";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	protected Properties properties;
	protected Map<String,Integer> positions;          // Motor position by joint name
	
	public MessageBottle() {
		this.properties = new Properties();
		this.positions = new HashMap<>();
	}

	public Properties getProperties() {return this.properties;}
	
	public String getProperty(String key,String defaultValue) {
		return this.properties.getProperty(key, defaultValue);
	}
	
	public void setProperty(String key,String value) {
		this.properties.setProperty(key, value);
	}
	/**
	 * Get the current position of the named joint. 
	 * @param key joint name
	 * @return the position, else 0 if the joint is not found.
	 */
	public int getPosition(String key) {
		Integer value = this.positions.get(key);
		if( value != null ) return value.intValue();
		return 0;
	}
	/**
	 * Define the current position of the motor in the named joint.
	 * Throw an illegal argument exception if the joint does not exist.
	 * @param key
	 * @param value
	 */
	public void setPosition(String key,int value) {
		Integer intValue = this.positions.get(key);
		if( intValue!=null ) {
			this.positions.put(key, value);
		}
		else {
			throw new IllegalArgumentException("Setting position: "+key+" does not exist");
		} 
	}
	/**
	 * Set the position map all at once.
	 * @param positions the new position map.
	 */
	public void setPositions(Map<String,Integer> positions) { this.positions = positions; }
	
	
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
	 * Convenience method to retrieve the ControllerType of the message source.
	 * 
	 * @return a source name. If there is no identified source, return null.
	 */
	public String getSource() {
		return this.properties.getProperty(BottleConstants.PROPERTY_SOURCE);
	}
	
	/**
	 * Convenience method to set a string naming the message creator. Use
	 * the controller type for this.
	 * 
	 * @param source the name of the message creator.
	 */
	public void setSource(String source) {
		this.properties.setProperty(BottleConstants.PROPERTY_SOURCE, source);
	}
	
	// =================================== JSON ======================================
	public static MessageBottle fromJSON(String json) {
		MessageBottle bottle = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			bottle = mapper.readValue(json,MessageBottle.class);
			
		}
		catch(JsonParseException jpe) {
			LOGGER.severe(String.format("%s.fromJSON: Parse exception (%s) from %s",CLSS,jpe.getLocalizedMessage(),json));
		}
		catch(JsonMappingException jme) {
			LOGGER.severe(String.format("%s.fromJSON: Mapping exception (%s) from %s",CLSS,jme.getLocalizedMessage(),json));
		}
		catch(IOException ioe) {
			LOGGER.severe(String.format("%s.fromJSON: IO exception (%s)",CLSS,ioe.getLocalizedMessage()));
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
			LOGGER.severe(String.format("%s.toJSON: Exception (%s)",CLSS,ex.getLocalizedMessage()));
		}
		return json;
	}
}
