/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.main;

import chuckcoughlin.bert.robot.Humanoid;



public class bert_runner {
	private final static String CLSS = "bert_runner";
	private static final String USAGE = "Usage: bert_runner";
	
	private final Humanoid robot;
	
	
	public bert_runner() {
		this.robot = Humanoid.getInstance();
	}


	/**
	 * Entry point for the application that contains the robot Java
	 * code for control of the appendages. 
	 * 
	 * Usage: bert_runner 
	 * 
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
			
		// No arguments for the time being
		if( args.length < 0) {
			System.out.println(USAGE);
			System.exit(1);
		}
        
        bert_runner runner = new bert_runner();

  
	}

}
