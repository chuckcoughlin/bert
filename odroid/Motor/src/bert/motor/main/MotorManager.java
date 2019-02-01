/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.main;

import java.util.Map;
import java.util.Properties;
/**
 *  This interface is satisfied by the MotorManager and describes
 *  the callback utilized by MotorContrtollers
 */
public interface MotorManager  {
	public void collectPositions(Map<Integer,Integer> map);
	public void collectProperties(Properties props);
	public int getControllerCount();
}
