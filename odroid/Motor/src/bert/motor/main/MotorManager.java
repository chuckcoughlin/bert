/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.main;

import bert.share.message.MessageBottle;
/**
 *  This interface is satisfied by the MotorManager and describes
 *  the callback utilized by MotorContrtollers
 */
public interface MotorManager  {
	public void handleAggregatedResponse(MessageBottle response);
	public void handleSynthesizedResponse(MessageBottle response);
	public void handleSingleMotorResponse(MessageBottle response);
	public int getControllerCount();
}
