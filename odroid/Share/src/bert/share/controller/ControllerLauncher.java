/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import bert.share.bottle.ResponseBottle;
/**
 *  This interface describes the parent process that launches a controller.
 *  The method is a callback to be used on completion. The key is an identifying
 *  string given the controller on its instantiation.
 */
public interface ControllerLauncher  {
	public void handleResult(String key,ResponseBottle response);
}
