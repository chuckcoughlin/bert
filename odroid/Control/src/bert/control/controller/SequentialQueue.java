/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.control.controller;

import java.util.LinkedList;
import java.util.logging.Logger;

import bert.control.message.InternalMessage;

/**
 *  Holds a list of requests to be executed in sequence. In general,
 *  requests in the same queue affect the same robot sub-chain of motors.
 */
public class SequentialQueue extends LinkedList<InternalMessage>  {
	private static final long serialVersionUID = -3633729383458991404L;
	protected static final String CLSS = "SequentialQueue";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private boolean inProgress;
	private long nextAllowedExecuteTime = 0;
	
	/**
	 * Constructor:
	 * @param launcher the launcher parent process
	 */
	public SequentialQueue() {
		this.inProgress = false;
		this.nextAllowedExecuteTime = System.nanoTime()/1000000;   // Work in milliseconds
	}
	
	public void setInProgress(boolean flag) { this.inProgress=flag; }
	public boolean isInProgress() { return this.inProgress; }
	
	/**
	 * Add the specified message to the end of the queue. Set the execution time
	 * respecting the delay setting. 
	 */
	@Override
	public void addLast(InternalMessage msg) {
		super.addLast(msg);
		long now = System.nanoTime()/1000000;
		msg.setExecutionTime(now+msg.getDelay());
	}
	
	/**
	 * Remove the next message from the queue in preparation for adding it
	 * to the timed delay queue. Update the time at which we are allowed to trigger the next message.
	 */
	@Override
	public InternalMessage removeFirst() {
		InternalMessage msg = super.removeFirst();
		long now = System.nanoTime()/1000000;
		if(nextAllowedExecuteTime < now) nextAllowedExecuteTime = now;
		if( msg.getExecutionTime() < nextAllowedExecuteTime ) {
			msg.setExecutionTime(nextAllowedExecuteTime);
		}
		nextAllowedExecuteTime = msg.getExecutionTime()+msg.getDuration();
		return msg;
	}
	
}
