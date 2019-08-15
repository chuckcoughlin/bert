/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.controller;

import bert.share.message.MessageBottle;

/**
 * Wrap a request/response message while processing within the MotorController.
 * The ultimate purpose is to attach a serial message response count to the
 * request so that we can determine when the response is complete.
 * @author chuckc
 *
 */
public class MessageWrapper {
	private final MessageBottle message;
	private int responseCount;
	public MessageWrapper(MessageBottle msg) {
		this.message = msg;
		this.responseCount = 1;
	}
	public MessageBottle getMessage() { return this.message; }
	/**
	 * The response count indicates the number of serial responses
	 * yet expected. When this count is zero, the response is 
	 * ready to be sent along to the group controller.
	 * @return the remaining count
	 */
	public int getResponseCount() { return this.responseCount; }
	public void setResponseCount(int count) { this.responseCount=count; }
	public void decrementResponseCount() { this.responseCount = responseCount-1; }
}
