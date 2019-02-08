/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import bert.share.message.MessageBottle;

/**
 *  A common interface for controllers owned by application instances.
 */
public interface Controller  {
	public void receiveRequest(MessageBottle request);
	public void receiveResponse(MessageBottle response);
	public void start();
	public void stop();
}
