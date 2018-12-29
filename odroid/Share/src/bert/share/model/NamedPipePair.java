/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.model;

import bert.share.common.PipeDirection;
import bert.share.common.PipeMode;

/**
 *  This class encapsulates two named pipes for bi-directional
 *  communication.
 */
public class NamedPipePair   {
	private final boolean master;  // True if this instance is owned by the server.
	private PipeDirection direction = PipeDirection.CLIENT_TO_SERVER;
	private PipeMode mode = PipeMode.ASYNCHRONOUS;
	private String name;
	
	public NamedPipePair(boolean isMaster) {
		String name = "";
		this.master = isMaster;
	}
	
	public boolean isMaster() {return this.master;}
	public PipeMode getMode() {return this.mode;}
 	public void setMode(PipeMode pmode) {this.mode=pmode;}
	public String getName() {return this.name;}
 	public void setName(String nam) {this.name=nam;}
 	public PipeDirection getDirection() {return this.direction;}
 	public void setDirection(PipeDirection dir) {this.direction=dir;}
	
	/**
	 * Create the pair of named pipes if they don't already exist.
	 * It is always the server-side that does the creating.
	 * @return true if the pipes are ready for reading and writing.
	 */
	public boolean createPipes() {
		return true;
	}
	
	public String read() {
		return "";
	}
	
	public void write(String data) {
		
	}
}

