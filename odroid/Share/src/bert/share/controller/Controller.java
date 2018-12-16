/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import bert.share.model.NamedPipePair;

/**
 *  This interface describes a controller that holds a  list of motors.
 */
public interface Controller  {
	public void setPipe(NamedPipePair pipe);
}
