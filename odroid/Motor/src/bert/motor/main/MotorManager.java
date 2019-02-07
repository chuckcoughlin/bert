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
	public void collectPositions(Map<Integer,Integer> map);
	public void collectProperties(Map<String,String> props);
	public int getControllerCount();
}
