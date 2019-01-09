/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import bert.share.common.NamedPipePair;
import bert.share.model.AbstractRobotModel;

/**
 *  This interface describes a controller that holds a  list of motors.
 */
public interface Controller  {
	public void configure(AbstractRobotModel model);
	public void setPipe(NamedPipePair pipe);
}
