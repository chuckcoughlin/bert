/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.main;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bert.motor.model.RobotMotorModel;
import bert.share.bottle.BottleConstants;
import bert.share.bottle.MessageBottle;
import bert.share.bottle.MetricType;
import bert.share.bottle.RequestType;
import bert.share.common.DynamixelType;
import bert.share.common.NamedPipePair;
import bert.share.common.Port;
import bert.share.controller.CommandController;
import bert.share.motor.Joint;
import bert.share.motor.JointProperty;
import bert.share.motor.MotorConfiguration;

/**
 * The MotorHandler receives requests across the pipe from the dispatcher,
 * formulates a serial command and delivers it to the motors. It then requests
 * motor status and formulates the reply.
 * 
 * Each controller handles a group of motors that are all on the same
 * serial port. For each request there is a single response. Responses
 * are synchronous.
 */
public class MotorManager {
	private final static String CLSS = "MotorHandler";
	private static final String USAGE = "Usage: motors <robot_root>";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private final RobotMotorModel model;
	private final Map<String,Thread> motorGroupHandlers;

	
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public MotorManager(RobotMotorModel m) {
		this.model = m;
		this.motorGroupHandlers = new HashMap<>();
		
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
			Port port = model.getPortForGroup(group);
			PortHandler handler = new PortHandler(group,port);
			Thread t = new Thread(handler);
			motorGroupHandlers.put(group, t);
			
			// Add all the motor configurations to the handler
			List<String> jointNames = model.getJointNamesForGroup(group);
			for( String jname:jointNames ) {
				MotorConfiguration motor = motors.get(jname.toUpperCase());
				handler.putMotorConfiguration(jname, motor);
			}
		}
	}
	
	public void start() {
		for( Thread t:motorGroupHandlers.values()) {
			t.start();
		}
	}

	/** 
	 * Who calls this?
	 */
	public void stop() {
		for( Thread t:motorGroupHandlers.values()) {
			t.interrupt();
		}
	}

	/**
	 * There are a few requests that we can process immediately.
	 * The majority require that we send the request to all message
	 * groups and wait for replies from all before responding.
	 * In the latter case, the responses include current positions
	 * of all joints.
	 * 
	 * @param request
	 * @return the response, usually containing current joint positions.
	 */
	public MessageBottle processRequest(MessageBottle request) {
		if( canHandleImmediately(request) ) {
			MessageBottle response = createResponseForLocalRequest(request);
			return response;
		}
		// Post requests to all motor group threads. The thread that can handle
		// the request will do so. Merge results into the original request and respond.
		return null;
	}

	// =========================== Private Helper Methods =======================================
	// Queries of fixed properties of the motors are the kinds of requests that can be handled immediately
	private boolean canHandleImmediately(MessageBottle request) {
		if( request.getRequestType().equals(RequestType.GET_PROPERTY)) {
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
		if( request.getRequestType().equals(RequestType.GET_PROPERTY)) {
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
