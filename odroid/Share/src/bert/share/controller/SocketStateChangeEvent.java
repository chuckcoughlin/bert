/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import java.util.EventObject;

/**
 * This is asa custom event class that conveys information aout the state of a socket.
 * 
 */
public class SocketStateChangeEvent extends EventObject {
	private static final long serialVersionUID = -7642269391076595297L;
	private final String name;
	private final String state;
	
	// Recognized state names
	public final static String READY = "ready";

	/**
	 * Constructor. Value is a simple object (not null,not a QualifiedValue)
	 * @param source the event originator
	 * @param name the name of the connection that originated the event
	 * @param state the new state taken from one of the defined values in this class
	 */
	public SocketStateChangeEvent(Object source,String name,String state)  {
		super(source);
		this.name = name;
		this.state = state;
	}
	
	public String getName() { return this.name; }
	public String getState() { return this.state; }
}
