/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * MIT License
 */
package bert.share.bottle;

import java.util.UUID;

/**
 * A RepeatingMessageBottle is a message, usually a request that is
 * configured to be submitted repeatedly via a RequestTimer. It contains
 * additional members associated with timing.
 */
public class RepeatingMessageBottle extends MessageBottle {
	private static final long serialVersionUID = 4356286171135500644L;
	private static final String CLSS = "RepeatingMessageBottle";
	private long expiration = 0;
	private final UUID uuid;
	
	public RepeatingMessageBottle() {
		this.uuid = UUID.randomUUID();
	}

	/**
	 * The expiration is the time that this message will expire
	 * relative to the run-time of the virtual machine.
	 * @return current expiration time ~ msecs.
	 */
	public long getExpiration() { return expiration;}
	public UUID getUUID() { return uuid; }
	
	/**
	 * Set the number of millisecs into the future for this message to expire.
	 * If we've already "executed in the future" due to speedup, then 
	 * use that time as the threshold.
	 * @param delay ~ msecs
	 */
	public void setDelay(long delay) {
		long now = System.nanoTime()/1000000;
		this.expiration = delay + now; 
	}
	/**
	 * Set the number of secs into the future for this message to expire.
	 * This is a convenience method to avoid the constant time 
	 * conversions when dealing with seconds.
	 * @param delay ~ secs
	 */
	public void setSecondsDelay(double delay) {
		setDelay((long)(delay*1000)); 
 
	}
	public void decrementExpiration(long delta) { this.expiration = expiration-delta; if( expiration<0) expiration=0;}
	
	/**
	 * Two messages are equal if their Ids are equal
	 */
	@Override
	public boolean equals(Object object){
		if(object instanceof RepeatingMessageBottle && ((RepeatingMessageBottle)object).getUUID() == this.uuid){
		    return true;
		} 
		else {
		    return false;
		}
	}
	// If we override equals, then we also need to override hashCode()
	@Override
	public int hashCode() {
		return getUUID().hashCode();
	}
	
	@Override
	public String toString() {
		return String.format("%s: expires in %d ms",CLSS,getExpiration()-System.nanoTime()/1000000);
	}
}
