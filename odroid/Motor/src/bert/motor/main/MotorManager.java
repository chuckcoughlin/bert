/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.main;

import java.util.Map;
/**
 *  This interface is satisfied by the MotorManager and describes
 *  the callback utilized by MotorContrtollers
 */
public interface MotorManager  {
	public void aggregateMotorProperties(Map<Integer,String> map);
	public void handleUpdatedProperties(Map<String,String> updates);
	public int getControllerCount();
}
