/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.message;

import bert.share.message.MessageBottle;
/**
 *  This interface describes the central processes that launch controllers
 *  and manage communications between them.
 *  The request methods are synchronous; the handler processes one request at a time. 
 *  The response method is expected to be a callback used on completion of the
 *  current request.
 */
public interface MessageHandler  {
	/**
	 * Controllers handle communication with peripheral entities. Instances
	 * specific to this application are created here.
	 */
	public void createControllers();
	public void execute();
	public void handleRequest(MessageBottle request);
	public void handleResponse(MessageBottle response);
	public void initialize();
	public void start();
	public void stop();
}