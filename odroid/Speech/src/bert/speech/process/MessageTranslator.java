/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.speech.process;

import java.util.logging.Logger;

import bert.share.message.BottleConstants;
import bert.share.message.MessageBottle;
import bert.share.message.MetricType;
import bert.share.message.RequestType;
import bert.share.motor.Joint;


/**
 *  This translator takes "Request Bottles" and generate text meant to be spoken.
 *  
 */
public class MessageTranslator  {
	private static final String CLSS = "MessageTranslator";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	
	/**
	 * Constructor.
	 */
	public MessageTranslator() {
	}
	

	/**
	 * In many cases, text is set in the Dispatcher or MotorController. Use those
	 * versions preferentially.
	 * @param msg the response
	 * @return pronounceable text
	 */
	public String messsageToText(MessageBottle msg) {
		String text = null;
		if( msg!=null  ) {
			String error = msg.fetchError();
			if( error!=null && !error.isEmpty() ) {
				text = error;
			}
			if( text==null || text.isEmpty() ) {
				text = msg.getProperty(BottleConstants.TEXT,"");
			}
			
			if( text==null || text.isEmpty() ) {
				RequestType type = msg.fetchRequestType();
				if(type.equals(RequestType.NOTIFICATION)) {
					text = "Received empty notification.";
				}
				// We expect the Dispatcher to fill these in ...
				else if(type.equals(RequestType.GET_METRIC)) {
					MetricType metric = MetricType.valueOf(msg.getProperty(BottleConstants.METRIC_NAME, "NAME"));
					text = String.format("The metric %s is unknown", metric.name().toLowerCase());
				}
				else if(type.equals(RequestType.GET_CONFIGURATION)) {
					text = "Motor metrics have been written to log files";
				}
				// We expect the MotorGroupController to fill these in ...
				else if(type.equals(RequestType.GET_MOTOR_PROPERTY)) {
					String propertyName = msg.getProperty(BottleConstants.PROPERTY_NAME, "");
					Joint joint   = Joint.valueOf(msg.getProperty(BottleConstants.JOINT_NAME, "UNKNOWN").toUpperCase());
					String value       = msg.getProperty(propertyName, "");
					text = String.format("The %s of my %s is %s", propertyName,Joint.toText(joint),value);
				}
				else if(type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
					String propertyName = msg.getProperty(BottleConstants.PROPERTY_NAME, "");
					text = String.format("Motor %s have been written to log files", propertyName.toLowerCase());
				}
				else {
					String property = msg.getProperty(BottleConstants.PROPERTY_NAME, "unknown");
					String value = msg.getProperty(property, "unknown");
					text = String.format("My %s is %s", property,value);
				}
			}
		}
		else {
			text = "I received an empty message";
		}
		if(text==null || text.isEmpty()) text = "I don't understand the response";
		return text;
	}
	
	// ============================================== Helper Methods ===================================

}
