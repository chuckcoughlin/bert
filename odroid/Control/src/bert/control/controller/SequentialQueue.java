/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.control.controller;

import java.util.LinkedList;
import java.util.logging.Logger;

import bert.control.message.InternalMessageHolder;

/**
 *  Holds a list of requests to be executed in sequence. In general,
 *  requests on the same queue affect the same robot sub-chain of motors.
 *  
 *  The inProgress flag true means that there is a message from the queue
 *  currently being executed by the dispatcher. This is used to prevent
 *  immediate execution of a new request if inappropriate.
 */
public class SequentialQueue extends LinkedList<InternalMessageHolder>  {
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
	public void addLast(InternalMessageHolder holder) {
		super.addLast(holder);
		long now = System.nanoTime()/1000000;
		holder.setExecutionTime(now+holder.getDelay());
	}
	
	/**
	 * Remove the next holder from the queue in preparation for adding it
	 * to the timed delay queue. Update the time at which we are allowed to trigger the next message.
	 */
	@Override
	public InternalMessageHolder removeFirst() {
		InternalMessageHolder holder = super.removeFirst();
		long now = System.nanoTime()/1000000;
		if(nextAllowedExecuteTime < now) nextAllowedExecuteTime = now;
		if( holder.getExecutionTime() < nextAllowedExecuteTime ) {
			holder.setExecutionTime(nextAllowedExecuteTime);
		}
		nextAllowedExecuteTime = holder.getExecutionTime()+holder.getMessage().getDuration();
		return holder;
	}
	
}
