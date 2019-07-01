/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.control.controller;

import java.util.LinkedList;
import java.util.logging.Logger;

import bert.control.message.SequentialMessage;

/**
 *  Holds a list of requests to be executed in sequence.
 */
public class SequentialQueue extends LinkedList<SequentialMessage>  {
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
	 * We assume that the message is being removed to be executed.
	 * Update the time at which we are allowed to trigger the next message.
	 */
	@Override
	public SequentialMessage removeFirst() {
		SequentialMessage msg = super.removeFirst();
		long now = System.nanoTime()/1000000;
		if(nextAllowedExecuteTime < now) nextAllowedExecuteTime = now;
		msg.setExecutionTime(nextAllowedExecuteTime);
		nextAllowedExecuteTime += msg.getDuration();
		return msg;
	}
	
}
