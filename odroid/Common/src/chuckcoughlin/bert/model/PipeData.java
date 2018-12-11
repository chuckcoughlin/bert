/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.bert.model;

/**
 *  This class maintains parameters for a single named pipe.
 */
public class PipeData   {
	private PipeDirection direction = PipeDirection.INPUT;
	private PipeMode mode = PipeMode.ASYNCHRONOUS;
	private String name;
	
	public PipeData() {
		String name = "";
	}
	
	public PipeMode getMode() {return this.mode;}
 	public void setMode(PipeMode pmode) {this.mode=pmode;}
 	public PipeDirection getDirection() {return this.direction;}
 	public void setDirection(PipeDirection dir) {this.direction=dir;}


}

