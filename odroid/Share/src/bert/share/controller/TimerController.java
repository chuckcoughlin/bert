/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.share.controller;

import java.util.logging.Logger;

import bert.share.controller.Controller;
import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;
import bert.share.message.RepeatingMessageBottle;

/**
 *  A timer controller accepts a RequestBottle and submits it to the parent
 *  Dispatcher on a clocked basis. The initial implementation handles only a
 *  single request.
 */
public class TimerController implements Controller  {
	protected static final String CLSS = "TimerController";
	private Logger LOGGER = Logger.getLogger(CLSS);
	private final long cadence;  // ~ msecs
	private RepeatingMessageBottle request = null;
	private final MessageHandler dispatcher;
	private final RepeatingMessageTimer timer;
	
	/**
	 * Constructor:
	 * @param launcher the launcher parent process
	 * @param interval repeat interval for request submission
	 */
	public TimerController(MessageHandler launcher,int interval) {
		this.cadence = interval;
		this.dispatcher = launcher;
		this.timer = new RepeatingMessageTimer(dispatcher,cadence);
	}
	
	public void setRequest(RepeatingMessageBottle msg) { 
		this.request = msg; 
		request.setDelay(cadence);  // Initial delay
		timer.addMessage(msg);
	}

	@Override
	public void receiveRequest(MessageBottle request) {
		
	}
	/**
	 * Response messages are reported to the launcher 
	 * directly by the timer.
	 */
	@Override
	public void receiveResponse(MessageBottle response) {
		dispatcher.handleResponse(response);
	}
	@Override
	public void start() {
		timer.start();
	}
	@Override
	public void stop() {
		timer.stop();
	}
}
