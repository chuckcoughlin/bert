/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.controller;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import bert.control.message.InternalMessage;
import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;
import bert.share.message.RequestType;

/**
 *  Patterned after a watchdog timer which manages a collection of "watchdogs". The dogs
 *  are sorted by expiration time. "petting" a dog resets the timeout
 *  perhaps indefinitely. Once the petting stops, the dog's "evaluate"
 *  method is invoked. There is always, at least one dog present in
 *  the list, the IDLE dog.
 */
public class TimedQueue extends LinkedList<InternalMessage> implements Runnable   {
	private static final long serialVersionUID = -5509446352724816963L;
	private final static String CLSS = "TimedQueue";
	private final static int IDLE_DELAY = 60000;    // One minute
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	protected boolean stopped = true;
	protected Thread timerThread = null;
	protected final InternalMessage idleMessage;
	protected long currentTime = 0;
	protected String name = CLSS;
	private final MessageHandler dispatcher;

	/**
	 * Constructor: At the specified time, the next message is sent to the laumcher.
	 * @param launcher parent launcher
	 */
	public TimedQueue(MessageHandler launcher)  {
		this.dispatcher = launcher;
		this.idleMessage = new InternalMessage(RequestType.IDLE);
		idleMessage.setShouldRepeat(true);
		idleMessage.setRepeatInterval(IDLE_DELAY);
	}


	/**
	 * Add a new message to the list ordered by its absolute execution time
	 * which must be already set. 
	 * The list is never empty, there is at least the IDLE message.
	 * @param msg message to be added
	 */
	public void addMessage(final InternalMessage msg) {
		if(msg==null)  return;   // Ignore
		 insertMessage(msg);
	}
	public String getName()   { return this.name; }
	
	/**
	 * Insert a new message into the list in execution order.
	 * This list is assumed never to be empty
	 */
	private void insertMessage(InternalMessage msg) {
		int index=0;
		Iterator<InternalMessage> iter = iterator();
		while(iter.hasNext() ) {
			InternalMessage im = iter.next();
			if(im.getExecutionTime()>msg.getExecutionTime()) {
				add(index, msg);
				long now = System.nanoTime()/1000000;
				LOGGER.info(String.format("%s.insertMessage: %s scheduled in %d msecs position %d",
						CLSS,((MessageBottle)msg).fetchRequestType().name(),msg.getExecutionTime()-now,index));
				if( index==0) timerThread.interrupt();   // We've replaced the head
				return;
			}
			index++;
		}
		addLast(msg);
	}

	/**
	 * If top message is the IDLE messsage, then simply "pet" it.
	 * Otherwise pop the top msg and inform the launcher to execute.
	 */
	private synchronized void fireExecutor() {
		InternalMessage msg = removeFirst();
		if( msg.fetchRequestType().equals(RequestType.IDLE) ) {
			long now = System.nanoTime()/1000000;
			msg.setExecutionTime(now+msg.getRepeatInterval());
			add(msg);
		}
		else {
			LOGGER.info(String.format("%s.fireExecutor: dispatching %s ...",CLSS,((MessageBottle)msg).fetchRequestType().name()));
			dispatcher.handleRequest((MessageBottle)msg);
		}
		timerThread.interrupt();
	}

	/**
	 * This is for a restart. Use a new thread.
	 */
	public synchronized void start() {
		if( stopped ) {
			clear();
			addFirst(idleMessage);
			stopped = false;
			timerThread = new Thread(this, CLSS);
			timerThread.setDaemon(true);
			timerThread.start();
			LOGGER.info(String.format("%s.START timer thread %s (%d)",name,timerThread.getName(),timerThread.hashCode()));
		}
	}

	/**
	 * On stop, set all the msgs to inactive.
	 */
	public synchronized void stop() {
		if( !stopped ) {
			stopped = true;
			if(timerThread!=null) {
				timerThread.interrupt();
			}
		}
	}
	
	/**
	 * A timeout causes the head to be notified, then pops up the next dog. 
	 */
	public synchronized void run() {
		while( !stopped  ) {
			long now = System.nanoTime()/1000000;   // Work in milliseconds
			InternalMessage head = getFirst();
			long waitTime = (long)(head.getExecutionTime()-now);
			try {
				if( waitTime>0 ) {
					wait(waitTime);
				}
				currentTime = head.getExecutionTime();
				if (!stopped) fireExecutor();
			} 
			// An interruption allows a recognition of re-ordering the queue
			catch (InterruptedException e) {
				//LOGGER.info(String.format("%s.run: wait interrupted ---",getName()));
			}
			catch( Exception ex ) {
				LOGGER.log(Level.SEVERE,String.format("%s.Exception during timeout processing (%s)",CLSS,ex.getLocalizedMessage()),
						ex); 
			} 
		}
	}
}
