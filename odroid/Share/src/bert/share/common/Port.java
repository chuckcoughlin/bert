/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.common;

/**
 *  This class encapsulates a serial port.
 */
public class Port   {
	private static final String CLSS = "Port";
	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private final String name;
	private String device="";


	/**
	 * Constructor
	 * @param name root name of the pipe-pair.
	 * @param isOwner the Dispatcher "owns" the pipes.
	 */
	public Port(String name,String dev) {
		this.name = name;
		this.device = dev;

	}
	
	public String getDevice() {return this.device;}
	public String getName() {return this.name;}
 	
}

