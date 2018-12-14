/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.bert.main;

import java.util.ArrayList;
import java.util.List;

import chuckcoughlin.bert.controller.CommandController;
import chuckcoughlin.bert.controller.StatusController;
import chuckcoughlin.bert.model.Motor;

/**


/**
 *  This class is a singleton and represents the robot.
 */
public class Humanoid {
	private final static String CLSS = "Humanoid";
	private static Humanoid instance = null;
	private final StatusController upperController;
	private final CommandController lowerController;

	/**
	 * Constructor is private per Singleton pattern.
	 */
	private Humanoid() {
		List<Motor> lowerMotors = new ArrayList<>();
		List<Motor> upperMotors = new ArrayList<>();
		this.lowerController = new CommandController(lowerMotors);
		this.upperController = new StatusController(upperMotors);
	}

	/**
	 * Static method to create and/or fetch the single instance.
	 */
	public static Humanoid getInstance() {
		if( instance==null) {
			synchronized(Humanoid.class) {
				instance = new Humanoid();
			}
		}
		return instance;
	}


}

