/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bert.motor.model.RobotMotorModel;
import bert.share.bottle.BottleConstants;
import bert.share.bottle.MessageBottle;
import bert.share.bottle.RequestType;
import bert.share.common.DynamixelType;
import bert.share.motor.Joint;
import bert.share.motor.JointProperty;
import bert.share.motor.MotorConfiguration;
import jssc.SerialPort;

/**
 * The MotorHandler receives requests across the pipe from the dispatcher,
 * formulates a serial command and delivers it to the motors. It then requests
 * motor status and formulates the reply.
 * 
 * Each controller handles a group of motors that are all on the same
 * serial port. For each request there is a single response. Responses
 * are synchronous.
 */
public class MotorManager implements MotorManagerInterface {
	private final static String CLSS = "MotorHandler";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private final RobotMotorModel model;
	private final Map<String,PortHandler> motorGroupHandlers;
	private final Map<String,Thread> motorGroupThreads;
	private final ResponseAggregator aggregator;
	private Thread aggregatorThread;
	private MessageBottle request;
	private MessageBottle response;

	
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public MotorManager(RobotMotorModel m) {
		this.model = m;
		this.motorGroupHandlers = new HashMap<>();
		this.motorGroupThreads = new HashMap<>();
		this.request = null;
		this.response = null;
		this.aggregator = new ResponseAggregator(this);
	}

	/**
	 * This application is interested in the "serial" controllers. We can expect multiple
	 * instances. Assign each to a handler running in its own thread. The handler is aware
	 * of the individual motors in the group.
	 */
	public void createMotorGroups() {
		Set<String> groupNames = model.getControllerTypes().keySet();
		Map<String,MotorConfiguration> motors = model.getMotors(); 
		for( String group:groupNames ) {
			SerialPort port = model.getPortForGroup(group);
			PortHandler handler = new PortHandler(group,port,aggregator);
			Thread t = new Thread(handler);
			motorGroupHandlers.put(group, handler);
			motorGroupThreads.put(group, t);
			
			// Add all the motor configurations to the handler
			List<String> jointNames = model.getJointNamesForGroup(group);
			for( String jname:jointNames ) {
				MotorConfiguration motor = motors.get(jname.toUpperCase());
				handler.putMotorConfiguration(jname, motor);
			}
		}
	}
	
	@Override
	public int getGroupCount() { return motorGroupHandlers.size(); }
	public void start() {
		aggregatorThread = new Thread(aggregator);
		aggregatorThread.start();
		
		for( String key:motorGroupHandlers.keySet()) {
			PortHandler ph = motorGroupHandlers.get(key);
			ph.setStopped(false);
			ph.open();
			motorGroupThreads.get(key).start();
		}
	}

	/** 
	 * Called by Dispatcher when it is stopped.
	 */
	public void stop() {
		for( String key:motorGroupHandlers.keySet()) {
			PortHandler ph = motorGroupHandlers.get(key);
			ph.setStopped(true);
			ph.close();
			motorGroupThreads.get(key).interrupt();
		}
		aggregatorThread.interrupt();
	}

	/**
	 * There are 3 kinds of messages that we can process:
	 * 	1) Requests that can be satisfied from information in our static
	 *     configuration. Compose results and return immediately. 
	 *  2) Commands or requests that can be satisfied by a single port handler,
	 *     for example requesting status of commanding control of a single joint.
	 *     In this case, we blindly send the request to all port handlers, but
	 *     expect a reply from only one.
	 *  3) Global commands or requests. These apply to all motor groups. Examples
	 *     include setting a pose or commanding an action, requesting positional state.
	 *     These requests are satisfied by sending the request to all port handlers,
	 *     collecting responses from each and combining into a single reply.
	 * 
	 * @param request
	 * @return the response, usually containing current joint positions.
	 */
	public MessageBottle processRequest(MessageBottle request) {
		this.response = null;
		if( canHandleImmediately(request) ) {
			this.response = createResponseForLocalRequest(request);
		}
		else {
			// Post requests to all motor group threads. The thread(s) that can handle
			// the request will do so. Merge results in call-back and respond.
			for( String key:motorGroupHandlers.keySet()) {
				motorGroupHandlers.get(key).setRequest(request);
			}
			// The response is defined in the collectResult call-back
			synchronized(request) {
				try {
					request.wait();
				}
				catch(InterruptedException ie) {}
			}
		}
		return response;
	}
	// =========================== Motor Manager Interface =======================================
	/**
	 * This method is called externally by the object that merges serial responses into a cohesive
	 * overall answer.
	 * @param r the response
	 */
	public void collectResult(MessageBottle r) {
		this.response = r;
		this.request.notify();	
	}
	// =========================== Private Helper Methods =======================================
	// Queries of fixed properties of the motors are the kinds of requests that can be handled immediately
	private boolean canHandleImmediately(MessageBottle request) {
		if( request.getRequestType().equals(RequestType.GET_CONFIGURATION)) {
			// Certain properties are constants available from the configuration file.
			String property = request.getProperty(BottleConstants.PROPERTY_PROPERTY,"");
			if( property.equalsIgnoreCase(JointProperty.ID.name()) ||
				property.equalsIgnoreCase(JointProperty.MOTORTYPE.name()) ||
				property.equalsIgnoreCase(JointProperty.OFFSET.name()) ||
				property.equalsIgnoreCase(JointProperty.ORIENTATION.name()) ) {
				return true;
			}
		}
		return false;
	}

	// The "local" response is simply the original request with some text
	// to send directly to the user.
	private MessageBottle createResponseForLocalRequest(MessageBottle request) {
		if( request.getRequestType().equals(RequestType.GET_CONFIGURATION)) {
			JointProperty property = JointProperty.valueOf(request.getProperty(BottleConstants.PROPERTY_PROPERTY, ""));
			Joint joint = Joint.valueOf(request.getProperty(BottleConstants.PROPERTY_JOINT, "UNKNOWN"));
			String text = "";
			String jointName = Joint.toText(joint);
			MotorConfiguration mc = model.getMotors().get(joint.name());
			switch(property) {
			case ID:
				int id = mc.getId();
				text = "The id of my "+jointName+" is "+id;
				break;
			case MOTORTYPE:
				String modelName = "A X 12";
				if( mc.getType().equals(DynamixelType.MX28)) modelName = "M X 28";
				else if( mc.getType().equals(DynamixelType.MX64)) modelName = "M X 64";
				text = "My "+jointName+" is a dynamixel M X "+modelName;
				break;
			case OFFSET:
				double offset = mc.getOffset();
				text = "The offset of my "+jointName+" is "+offset;
				break;
			case ORIENTATION:
				String orientation = "indirect";
				if( mc.isDirect() ) orientation = "direct";
				text = "The orientation of my "+jointName+" is "+orientation;
				break;
			default:
				text = "";
				request.setError(property.name()+" is not a property that I can look up");
				break;
			}
			request.setProperty(BottleConstants.TEXT, text);
		}
		return request;
	}
}
