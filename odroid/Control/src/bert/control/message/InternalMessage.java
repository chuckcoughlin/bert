/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * MIT License
 */
package bert.control.message;

import bert.control.controller.QueueName;
import bert.share.common.BottleConstants;
import bert.share.message.HandlerType;
import bert.share.message.MessageBottle;
import bert.share.message.RequestType;

/**
 * Internal Messages are handled by the InternalController. These messages can
 * be queued, delayed and/or repeated.
 * All times are based on System time as obtained by: System.nanoTime()/1000000
 */
public class InternalMessage extends MessageBottle {
	private static final long serialVersionUID = 4356286171135500677L;
	private static final String CLSS = "SequentialMessage";
	private long delay = 0;
	private long executionTime = 0;
	private final QueueName queue; 
	private long repeatInterval = 1000;   // ~msecs
	private boolean repeat;
	
	/**
	 * Constructor for a message that is timed, but is not restricted to
	 * executing sequentially. 
	 * @param type
	 */
	public InternalMessage(RequestType type) {
		assignRequestType(type);
		assignSource(HandlerType.INTERNAL.name());
		this.queue = null;
	}
	/**
	 * Constructor for a message that is restricted to sequential execution
	 * after existing messages on its same FIFO queue.
	 * @param type
	 */
	public InternalMessage(RequestType type,QueueName q) {
		assignRequestType(type);
		assignSource(HandlerType.INTERNAL.name());
		this.queue = q;
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
	public QueueName getQueue() { return this.queue; }
	public long getRepeatInterval()             { return this.repeatInterval; }
	public void setRepeatInterval(long interval) { this.repeatInterval = interval; }
	public boolean shouldRepeat()             { return this.repeat; }
	public void setShouldRepeat(boolean flag) { this.repeat = flag; }
	

	/**
	 * CAUTION: We only do a shallow copy of the jointValues, for now. We preserve
	 *          all the properties except SOURCE.
	 * @param bottle the original message
	 * @param q queue on which to place this request for sequential execution
	 * @return
	 */
	public static InternalMessage clone(MessageBottle bottle,QueueName q) {
		InternalMessage msg = new InternalMessage(bottle.fetchRequestType(),q);
		msg.jointValues = bottle.getJointValues();  // Joint values are positions in a pose.
		for(String key:bottle.properties.keySet()) {
			if( !key.equalsIgnoreCase(BottleConstants.SOURCE) &&
				!key.equalsIgnoreCase(BottleConstants.TYPE)	) {
				msg.setProperty(key, bottle.getProperty(key, ""));
			}
		}
		return msg;
	}
	@Override
	public String toString() {
		return String.format("%s: expires in %d ms",CLSS,getExecutionTime()-System.nanoTime()/1000000);
	}
	
}
