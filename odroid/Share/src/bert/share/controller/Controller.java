/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

/**
 *  A common interface for controllers owned by application instances.
 */
public interface Controller  {
	public void start();
	public void stop();
}
