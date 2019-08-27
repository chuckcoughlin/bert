/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * MIT License
 */
package bert.control.message;

import bert.control.controller.QueueName;
import bert.share.message.MessageBottle;

/**
 * This is a base class for carriers of MessageBottles as they are processed by the 
 * InternalController. These come in two flavors - those that wait on a queue and 
 * those wait on a timer. The queued messages may optionally have a timed delay as well. 
 */
public class InternalMessageHolder {
	private static final long serialVersionUID = 4356286171135500677L;
	private static final String CLSS = "InternalMessageHolder";
	private static long id = 0;    // Sequential id for messages
	private long delay = 0;
	private long executionTime = 0;
	private final MessageBottle message;
	private final String originalSource;
	private QueueName queue; 
	private long repeatInterval = 1000;   // ~msecs
	private boolean repeat;
	
	public synchronized static long getNextId() { return ++id; }
	/**
	 * Constructor for the IDLE holder used by the timer. There is
	 * no message.
	 */
	public InternalMessageHolder() {
		this.message = null;
		this.queue = null;
		this.delay = 0;
		this.repeat = false;
		this.originalSource = null;
	}
	
	/**
	 * Constructor for a message that is timed, but is not restricted to
	 * executing sequentially. 
	 * @param msg the ultimate message to execute
	 * @param interval delay time ~msecs
	 */
	public InternalMessageHolder(MessageBottle msg,long interval) {
		this.message = msg;
		this.queue = null;
		this.delay = interval;
		this.repeat = false;
		this.originalSource = message.fetchSource();
		message.setId(getNextId());
	}
	/**
	 * Constructor for a message that is restricted to sequential execution
	 * after existing messages on its same FIFO queue.
	 * @param msg the message to execute when processing by the InternalController is complete.
	 */
	public InternalMessageHolder(MessageBottle msg,QueueName q) {
		this.message = msg;
		this.delay = 0;
		this.queue = q;
		this.repeat = false;
		this.originalSource = message.fetchSource();
		message.setId(getNextId());
		
	}
	/**
	 * The delay interval is an idle interval between when this message is
	 * first placed on the timer queue and when actually executes. Any time 
	 * spent waiting on the sequential queue is counted toward the delay
	 * ("time served").
	 * @return delay time ~ msecs.
	 */
	public long getDelay() { return delay;}
	public void setDelay(long time) { this.delay = time; }
	/**
	 * The execution time is earliest time at which this message is allowed
	 * to be sent to the Dispatcher. The time is calculated as the message
	 * is placed on the timer queue.
	 * @return current executionTime time ~ msecs.
	 */
	public long getExecutionTime() { return executionTime;}
	public void setExecutionTime(long time) { this.executionTime = time; }
	public MessageBottle getMessage() { return this.message; }
	public QueueName getQueue() { return this.queue; }
	public long getRepeatInterval()             { return this.repeatInterval; }
	public void setRepeatInterval(long interval) { this.repeatInterval = interval; }
	public boolean shouldRepeat()             { return this.repeat; }
	public void setShouldRepeat(boolean flag) { this.repeat = flag; }
	
	public void reinstateOriginalSource() {
		message.assignSource(originalSource);
	}
	
	@Override
	public String toString() {
		return String.format("%s: %s expires in %d ms",CLSS,message.fetchRequestType().name(),getExecutionTime()-System.nanoTime()/1000000);
	}
}
