/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.command.model;


/**
 *  This class is a singleton and represents the robot.
 */
public class Humanoid {
	private final static String CLSS = "Humanoid";
	private static Humanoid instance = null;


	/**
	 * Constructor is private per Singleton pattern.
	 */
	private Humanoid() {

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

