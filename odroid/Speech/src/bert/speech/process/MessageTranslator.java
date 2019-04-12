/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.speech.process;

import java.util.logging.Logger;

import bert.share.message.BottleConstants;
import bert.share.message.MessageBottle;
import bert.share.message.RequestType;


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
	

	public String messsageToText(MessageBottle msg) {
		String text = "I don't understand the response";
		if( msg!=null ) {
			String error = msg.fetchError();
			if( error!=null && !error.isEmpty() ) {
				text = error;
			}
			else if(msg.fetchRequestType().equals(RequestType.NOTIFICATION)) {
				text = msg.getProperty(BottleConstants.TEXT, "Received empty notification.");
			}
			else if(msg.fetchRequestType().equals(RequestType.GET_METRIC)) {
				text = msg.fetchError();
				if( text==null || text.isEmpty() ) {
					text = msg.getProperty(BottleConstants.TEXT, "??");
				}
			}
			else if(msg.fetchRequestType().equals(RequestType.GET_METRICS)) {
				text = msg.fetchError();
				if( text==null || text.isEmpty() ) {
					text = msg.getProperty(BottleConstants.TEXT, "??");
				}
			}
			else if(msg.fetchRequestType().equals(RequestType.LIST_MOTOR_PROPERTY)) {
				text = msg.fetchError();
				if( text==null || text.isEmpty() ) {
					text = msg.getProperty(BottleConstants.TEXT, "??");
				}
			}
			else {
				String property = msg.getProperty(BottleConstants.PROPERTY_NAME, "unknown");
				String value = msg.getProperty(property, "0");
				text = String.format("My %s is %s", property,value);
			}
		}
		else {
			text = "I received an empty message";
		}
		return text;
	}
}
