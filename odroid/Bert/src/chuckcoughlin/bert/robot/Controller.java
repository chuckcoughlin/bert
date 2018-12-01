/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.robot;

import java.util.List;


/**
 *  This interface describes a controller that holds a  list of motors.
 */
public interface Controller  {
	List<Motor> getMotors();
}
