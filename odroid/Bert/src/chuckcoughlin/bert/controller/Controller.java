/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.bert.controller;

import java.util.List;

import chuckcoughlin.bert.model.Motor;


/**
 *  This interface describes a controller that holds a  list of motors.
 */
public interface Controller  {
	List<Motor> getMotors();
}
