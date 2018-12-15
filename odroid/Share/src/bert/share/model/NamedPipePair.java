/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.model;

/**
 *  This class encapsulates two named pipes for bi-directional
 *  communication.
 */
public class NamedPipePair   {
	private final boolean master;
	private String name;
	
	public NamedPipePair(boolean isMaster) {
		String name = "";
		this.master = isMaster;
	}
	
	public boolean isMaster() {return this.master;}
	
	/**
	 * Create the pair of named pipes if they don't already exist.
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

