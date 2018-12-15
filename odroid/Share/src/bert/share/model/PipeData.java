/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.model;

import bert.share.common.PipeDirection;
import bert.share.common.PipeMode;

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

