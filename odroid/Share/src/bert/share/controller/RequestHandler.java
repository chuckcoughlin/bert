/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import bert.share.bottle.MessageBottle;
/**
 *  This interface describes the central applications that launch controllers
 *  and manage communications between them.
 *  The methods are synchronous; the handler processes one request at a time. 
 *  The response method is expected to be a callback used on completion of the
 *  current request.
 */
public interface RequestHandler extends Runnable  {
	public void createControllers();
	public void handleRequest(MessageBottle request);
	public void handleResponse(MessageBottle response);
	public void run();       // Runnable
	public void startup();
	public void shutdown();
}
