/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.server.timer;

import java.util.logging.Logger;

import bert.share.bottle.MessageBottle;
import bert.share.bottle.RepeatingMessageBottle;
import bert.share.controller.Controller;
import bert.share.controller.Dispatcher;

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
	private final Dispatcher dispatcher;
	private final RepeatingMessageTimer timer;
	
	/**
	 * Constructor:
	 * @param launcher the dispatcher parent process
	 * @param interval repeat interval for request submission
	 */
	public TimerController(Dispatcher launcher,int interval) {
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
	public void initialize() {
		
	}
	@Override
	public void receiveRequest(MessageBottle request) {
		
	}
	/**
	 * Response messages are reported to the dispatcher 
	 * directly by the timer.
	 */
	@Override
	public void receiveResponse(MessageBottle response) {
		
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
