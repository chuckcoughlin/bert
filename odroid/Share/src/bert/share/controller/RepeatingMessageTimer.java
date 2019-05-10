/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import bert.share.message.MessageHandler;
import bert.share.message.RepeatingMessageBottle;

/**
 *  Patterned after a watchdog timer which manages a collection of "watchdogs". The dogs
 *  are sorted by expiration time. "petting" a dog resets the timeout
 *  perhaps indefinitely. Once the petting stops, the dog's "evaluate"
 *  method is invoked. There is always, at least one dog present in
 *  the list, the IDLE dog.
 *  
 *  This is the production version of the timer. It does not allow
 *  for alteration of the time-scale.
 *  
 *  Interested entities register as TimeoutObservers. 
 */
public class RepeatingMessageTimer implements Runnable   {
	private final static String CLSS = "RepeatingMessageTimer";
	private final static int IDLE_DELAY = 60000;    // One minute
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	protected final LinkedList<RepeatingMessageBottle> msgs;
	protected boolean stopped = true;
	protected Thread watchdogThread = null;
	protected final RepeatingMessageBottle idleMessage;
	protected long currentTime = 0;
	protected long cadence = 1000;
	protected String name = CLSS;
	private final MessageHandler dispatcher;

	/**
	 * Constructor: This version of the constructor supplies a timer name.
	 * @param launcher parent launcher
	 * @param interval repeat interval ~msecs
	 */
	public RepeatingMessageTimer(MessageHandler launcher,long interval)  {
		this.dispatcher = launcher;
		this.cadence = interval;
		this.idleMessage = new RepeatingMessageBottle();
		idleMessage.setDelay(IDLE_DELAY);
		this.msgs = new LinkedList<RepeatingMessageBottle>();
	}


	/**
	 * Add a new timer to the list. It holds an absolute expiration time. 
	 * The list is never empty, there is at least the IDLE dog.
	 * @param msg message to be added
	 */
	public void addMessage(final RepeatingMessageBottle msg) {
		if(msg==null)  return;   // Ignore
		 insert(msg);
	}
	public String getName()   { return this.name; }

	/**
	 * Remove the specified message from the list.
	 * If this is the fist in the list, then restart
	 * the timer. We assume that the IDLE message will never
	 * be removed. 
	 * @param msg to be removed
	 */
	public synchronized void removeMessage(final RepeatingMessageBottle msg) {
		if( msg!=null) {
			int index = msgs.indexOf(msg);
			if( index>=0 ) {
				msgs.remove(index);
				if( index==0) {   // We popped the top
					watchdogThread.interrupt();
				}
			}
			else {
				LOGGER.warning(String.format("%s.removeMessage: Unrecognized message (%s)",name,msg.toString()));
			}
		}
	}
	
	/**
	 * "pet" a message.
	 * Change the time of a specified message. If the message
	 * is not currently in the list, insert it.
	 * @param dog the dog to update. It has already been set 
	 *        with the new expiration time. 
	 */
	public synchronized void updateMessage(final RepeatingMessageBottle msg) {
		if( msg==null ) return;
		int index = msgs.indexOf(msg);
		if( index>=0 ) {
			msgs.remove(index);
		}
		// Add message back in (or for the first time)
		// -- this may trigger an interrupt
		insert(msg);
	}
	
	/**
	 * Insert a new dog into the list in order.
	 * This list is assumed never to be empty
	 */
	protected void insert(RepeatingMessageBottle dog) {
		int index=0;
		for(RepeatingMessageBottle wd:msgs ) {
			if(dog.getExpiration()<wd.getExpiration()) {
				msgs.add(index, dog);
				if( index==0) watchdogThread.interrupt();   // We've replaced the head
				return;
			}
			index++;
		}
		msgs.addLast(dog);
	}

	/**
	 * If top dog is the IDLE dog, then simply "pet" it.
	 * Otherwise pop the top msg and inform the launcher.
	 * Then "pet" it. 
	 */
	protected void fireWatchdog() {
		RepeatingMessageBottle msg = msgs.pop();
		if( msg.equals(idleMessage) ) {
			idleMessage.setDelay(IDLE_DELAY);
		}
		else {
			dispatcher.handleRequest(msg);
			msg.setDelay(cadence);
		}
		updateMessage(msg);
	}

	/**
	 * This is for a restart. Use a new thread.
	 */
	public synchronized void start() {
		if( stopped ) {
			msgs.clear();
			msgs.push(idleMessage);
			stopped = false;
			watchdogThread = new Thread(this, CLSS);
			watchdogThread.setDaemon(true);
			watchdogThread.start();
			LOGGER.info(String.format("%s.START timer thread %s (%d)",name,watchdogThread.getName(),watchdogThread.hashCode()));
		}
	}

	/**
	 * On stop, set all the msgs to inactive.
	 */
	public synchronized void stop() {
		if( !stopped ) {
			stopped = true;
			if(watchdogThread!=null) {
				watchdogThread.interrupt();
			}
		}
	}
	
	/**
	 * A timeout causes the head to be notified, then pops up the next dog. 
	 */
	public synchronized void run() {
		while( !stopped  ) {
			long now = System.nanoTime()/1000000;   // Work in milliseconds
			RepeatingMessageBottle head = msgs.getFirst();
			long waitTime = (long)(head.getExpiration()-now);
			try {
				if( waitTime>0 ) {
					wait(waitTime);
				}
				currentTime = head.getExpiration();
				if (!stopped) fireWatchdog();
			} 
			// An interruption allows a recognition of re-ordering the queue
			catch (InterruptedException e) {
				LOGGER.info(String.format("%s.run: wait interrupted ---",getName()));
			}
			catch( Exception ex ) {
				LOGGER.log(Level.SEVERE,String.format("%s.Exception during timeout processing (%s)",CLSS,ex.getLocalizedMessage()),
						ex); 
			} 
		}
	}
}
