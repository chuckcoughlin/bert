/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * MIT License
 */
package bert.server.message;

import bert.share.message.HandlerType;
import bert.share.message.MessageBottle;

/**
 * A TimedMessage is a message, usually a request, that is
 * configured to be submitted repeatedly via a RepeatingMessageTimer. It contains
 * additional members associated with timing.
 */
public class TimedMessage extends MessageBottle implements InternalMessage {
	private static final long serialVersionUID = 4356286171135500644L;
	private static final String CLSS = "TimedMessage";
	private long executionTime  = 0;
	private long repeatInterval = 1000;   // ~msecs
	private boolean repeat;
	
	public TimedMessage() {
		this.repeat = false;
		assignSource(HandlerType.INTERNAL.name());
	}

	/**
	 * The execution time is the time at which this message is allowed
	 * to be sent to the Dispatcher.
	 * @return current executionTime time ~ msecs.
	 */
	public long getExecutionTime() { return executionTime;}
	public void setExecutionTime(long time) { this.executionTime = time; }
	
	public long getRepeatInterval()             { return this.repeatInterval; }
	public void setRepeatInterval(long interval) { this.repeatInterval = interval; }
	public boolean shouldRepeat()             { return this.repeat; }
	public void setShouldRepeat(boolean flag) { this.repeat = flag; }
	
	
	@Override
	public String toString() {
		return String.format("%s: executes in %d ms",CLSS,getExecutionTime()-System.nanoTime()/1000000);
	}
}
