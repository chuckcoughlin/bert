/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.config;

/**
 *  This class maintains parameters for a single named pipe.
 */
public class PipeData   {
	private PipeDirection direction = PipeDirection.INPUT;

	public PipeData() {
		
	}
	
 	public PipeDirection getDirection() {return this.direction;}
 	public void setDirection(PipeDirection dir) {this.direction=dir;}


}

