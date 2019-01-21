/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.main;

import bert.share.bottle.MessageBottle;
/**
 *  This interface is satisfied by the MotorManager and describes
 *  the callback utilized by the Correlator
 */
public interface MotorManagerInterface  {
	public void collectResult(MessageBottle response);
	public int getGroupCount();
}
