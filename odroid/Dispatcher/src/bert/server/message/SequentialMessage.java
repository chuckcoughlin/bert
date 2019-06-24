/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * MIT License
 */
package bert.server.message;

import bert.server.controller.QueueName;
import bert.share.message.HandlerType;
import bert.share.message.MessageBottle;
import bert.share.message.RequestType;

/**
 * This is a message designed to be placed on a queue and executed
 * in order, but not until its predecessor completes.
 */
public class SequentialMessage extends MessageBottle implements InternalMessage {
	private static final long serialVersionUID = 4356286171135500677L;
	private static final String CLSS = "SequentialMessage";
	private long executionTime = 0;
	private long messageInterval = 0;   // ~msecs
	private QueueName queue = QueueName.GLOBAL;

	
	public SequentialMessage(RequestType type,QueueName q) {
		assignRequestType(type);
		assignSource(HandlerType.INTERNAL.name());
		this.queue = q;
	}

	/**
	 * The execution time is the time at which this message is allowed
	 * to be sent to the Dispatcher.
	 * @return current executionTime time ~ msecs.
	 */
	public long getExecutionTime() { return executionTime;}
	public void setExecutionTime(long time) { this.executionTime = time; }
	public QueueName getQueue() { return this.queue; }
	public void setQueue(QueueName q) { this.queue = q; }
	
	public long getMessageInterval() { return this.messageInterval; }
	/**
	 * Set the number of millisecs past the execution time of this message
	 * that a subsequent message must wait before it can execute.
	 * @param delay ~ msecs
	 */
	public void setMessageInterval(long delay) { this.messageInterval = delay; }

	/**
	 * Set the number of seconds past the execution time of this message
	 * that a subsequent message must wait before it can execute.
	 * @param delay ~ msecs
	 */
	public void setSecondsMessageInterval(double delay) {
		setMessageInterval((long)(delay*1000)); 
 
	}
	
	@Override
	public String toString() {
		return String.format("%s: expires in %d ms",CLSS,getExecutionTime()-System.nanoTime()/1000000);
	}
}
