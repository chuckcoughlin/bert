/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.motor.main;

import java.util.HashMap;
import java.util.Map;

import bert.share.bottle.MessageBottle;

/**
 *  Handle call-backs from the serial port handlers. Aggregate the responses
 *  when appropriate. When all is complete execute a call-back on the 
 *  MotorManager with the result.
 */
public class ResponseAggregator implements Runnable, AggregatorInterface  {
	protected static final String CLSS = "ResponseAggregator";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private int groupsProcessed;
	private final MotorManagerInterface motorManager;
	private final Map<String,Integer> positions;
	private MessageBottle response;

	public ResponseAggregator(MotorManagerInterface mm) {
		this.motorManager = mm;
		this.response = null;
		this.positions = new HashMap<>();
		this.groupsProcessed = 0;
	}
	
	public void run() {
		while( !Thread.currentThread().isInterrupted() ) {
			try {
				positions.wait();
				motorManager.collectResult(response);
			}
			catch(InterruptedException ie) {}
			finally {
				positions.clear();
				response = null;
				groupsProcessed = 0;
			}
		}
	}
	
	// ============================== Aggregator Interface ===============================
	/**
	 * With this type of message, the response is the message without modification.
	 * Simply send it along.
	 * @param msg the response message from the pertinent SerialPortHandler
	 */
	public void collectSerialResult(MessageBottle msg) {
		response = msg;
		positions.notify();
    }
	/**
	 * With this type of message, we require execution of the call-back from each of the
	 * serial port handlers. After the last one has arrived, notify our runner.
	 * @param msg the response message from one of the SerialPortHandler
	 */
	public void collectPartialSerialResult(MessageBottle msg,Map<String,Integer> map) {
		// Use the first message received as the template
		if( response == null ) {
			response = msg;
		}
		for( String key:map.keySet() ) {
			Integer pos = map.get(key);
			response.setPosition(key, pos);
		}
		groupsProcessed++;
		if(groupsProcessed>=motorManager.getGroupCount() ) {
			positions.notify();
		}
	}
}
