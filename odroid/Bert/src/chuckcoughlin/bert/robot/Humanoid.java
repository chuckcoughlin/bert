/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.robot;/**

/**
 *  This class is a singleton and represents the robot.
 */
public class Humanoid {
	private final static String CLSS = "Humanoid";
	private static Humanoid instance = null;
	private final Controller upperController;
	private final Controller lowerController;
    
	/**
	 * Constructor is private per Singleton pattern.
	 */
	private Humanoid() {
		Controller lowerController = new Controller();
		Controller upperController = new Controller();
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

