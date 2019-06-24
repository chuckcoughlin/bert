/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * MIT License
 */
package bert.server.message;

/**
 * A InternalMessage is a message that can be placed on a TimedQueue.
 * All times are based on System time as obtained by: System.nanoTime()/1000000
 */
public interface InternalMessage  {
	/**
	 * @return the System time in milliseconds.
	 * 		
	 */
	public long getExecutionTime();
	public void setExecutionTime(long time); 
}
