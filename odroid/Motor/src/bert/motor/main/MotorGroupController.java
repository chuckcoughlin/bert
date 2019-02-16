/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import bert.motor.model.RobotMotorModel;
import bert.share.common.DynamixelType;
import bert.share.controller.Controller;
import bert.share.message.BottleConstants;
import bert.share.message.MessageBottle;
import bert.share.message.RequestType;
import bert.share.motor.Joint;
import bert.share.motor.JointProperty;
import bert.share.motor.MotorConfiguration;
import jssc.SerialPort;

/**
 * The MotorGroupController receives requests from the server having to do with
 * the Dynamixel motors. This controller dispenses the request to the multiple
 * MotorControllers receiving results via a call-back. An await-signal scheme
 * is used to present a synchronized method interface to the server.
 * 
 * On initialization, the system architecture is checked to determine if
 * this is being run in a development or production environment. If
 * development, then responses are simulated without any direct serial
 * requests being made.
 */
public class MotorGroupController implements Controller,MotorManager {
	private final static String CLSS = "MotorGroupController";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private final RobotMotorModel model;
	private final Map<String,MotorController> motorControllers;
	private final Map<Integer,String> motorNameById;
	private final Map<String,Thread> motorControllerThreads;
	private MessageBottle request;
	private MessageBottle response;
	private final boolean development;
	private final Condition waiting;
	private final Lock lock;
	private int motorCount;
	private int motorsProcessed;

	private final Map<String,Integer> positionsInProcess;
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public MotorGroupController(RobotMotorModel m) {
		this.model = m;
		this.motorControllers = new HashMap<>();
		this.motorControllerThreads = new HashMap<>();
		this.request = null;
		this.response = null;
		this.motorCount = 0;
		this.motorsProcessed = 0;
		this.motorNameById = new HashMap<>();
		this.positionsInProcess = new HashMap<>();
		this.lock = new ReentrantLock();
		this.waiting = lock.newCondition();
		LOGGER.info(String.format("%s.constructor: os.arch = %s",CLSS,System.getProperty("os.arch")));  // x86_64
		LOGGER.info(String.format("%s.constructor: os.name = %s",CLSS,System.getProperty("os.name")));  // Mac OS X
		development = System.getProperty("os.arch").startsWith("x86");
	}

	/**
	 * Create the "serial" controllers that handle Dynamixel motors. We launch multiple
	 * instances each running in its own thread. Each controller handles a group of
	 * motors all communicating on the same serial port.
	 */
	public void initialize() {
		if( !development ) {
			Set<String> groupNames = model.getHandlerTypes().keySet();
			Map<String,MotorConfiguration> motors = model.getMotors(); 
			for( String group:groupNames ) {
				SerialPort port = model.getPortForGroup(group);
				MotorController controller = new MotorController(group,port,this);
				motorCount += controller.getMotorCount();
				Thread t = new Thread(controller);
				motorControllers.put(group, controller);
				motorControllerThreads.put(group, t);

				// Add configurations to the controller for each motor in the group
				List<String> jointNames = model.getJointNamesForGroup(group);
				for( String jname:jointNames ) {
					MotorConfiguration motor = motors.get(jname.toUpperCase());
					if( motor!=null ) {
						controller.putMotorConfiguration(jname, motor);
						motorNameById.put(motor.getId(), jname);
					}
					else {
						LOGGER.warning(String.format("%s.createMotorGroups: Motor %s not found in %s",CLSS,jname,group));
					}
				}
			}
		}
	}
	
	@Override
	public int getControllerCount() { return motorControllers.size(); }

	@Override
	public void start() {
		if(!development ) {
			for( String key:motorControllers.keySet()) {
				MotorController controller = motorControllers.get(key);
				controller.setStopped(false);
				controller.initialize();
				controller.start();
				motorControllerThreads.get(key).start();
			}
		}
	}

	/** 
	 * Called by Dispatcher when it is stopped.
	 */
	@Override
	public void stop() {
		if(!development ) {
			for( String key:motorControllers.keySet()) {
				MotorController controller = motorControllers.get(key);
				controller.setStopped(true);
				controller.stop();
				motorControllerThreads.get(key).interrupt();
			}
		}
	}

	/**
	 * There are 3 kinds of messages that we process:
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
			response = createResponseForLocalRequest(request);
		}
		else if(development ) {
			response = simulateResponseForRequest(request);
		}
		else {
			// Post requests to all motor controller threads. The thread(s) that can handle
			// the request will do so. Merge results in call-back and respond.
			lock.lock();
			motorsProcessed = 0;
			positionsInProcess.clear();
			for( String key:motorControllers.keySet()) {
				motorControllers.get(key).receiveRequest(request);
			}
			// The response becomes defined in the collectPositions() call-back
			// so is available for return.
			try {
				waiting.await();
			}
			catch(InterruptedException ie) {}
			
		}
		return response;
	}
	
	@Override
	public void receiveRequest(MessageBottle request) {}
	@Override
	public void receiveResponse(MessageBottle response) {}
	// =========================== Motor Manager Interface ====================================
	/**
	 * This method is called by the controller that handled a request that pertained to
	 * a single motor.
	 * @param props the properties modified by or read by the controller
	 */
	public void collectProperties(Map<String,String> props) {
		this.response = this.request;
		if( props!=null ) {
			for(Object key:props.keySet()) {
				String name = key.toString();
				this.response.setProperty(name, props.get(name));
			}
		}
		this.request.notify();	
	}
	/**
	 * This method is called by the serial controllers. Within them each individual motor generates
	 * a call. When we have heard from all of them, trigger a reply.
	 * @param map position of an individual motor.
	 */
	public void collectPositions(Map<Integer,Integer> map) {
		if( map!=null ) {
			for( Integer key:map.keySet() ) {
				Integer pos = map.get(key);
				String name = motorNameById.get(key);
				positionsInProcess.put(name, pos);
			}
		}
		motorsProcessed++;
		if(motorsProcessed>=motorCount ) {
			this.response = this.request;
			this.response.setPositions(positionsInProcess);
			waiting.signal();	
		}
	}
	// =========================== Private Helper Methods =====================================
	// Queries of fixed properties of the motors are the kinds of requests that can be handled
	// immediately
	private boolean canHandleImmediately(MessageBottle request) {
		if( request.fetchRequestType().equals(RequestType.GET_CONFIGURATION)) {
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
	// to send directly to the user. These values are obtained from the initial configuration.
	private MessageBottle createResponseForLocalRequest(MessageBottle request) {
		if( request.fetchRequestType().equals(RequestType.GET_CONFIGURATION)) {
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
				request.assignError(property.name()+" is not a property that I can look up");
				break;
			}
			request.setProperty(BottleConstants.TEXT, text);
		}
		return request;
	}
	// When in development mode, simulate something reasonable as a response.
	private MessageBottle simulateResponseForRequest(MessageBottle request) {
		RequestType requestType = request.fetchRequestType();
		if( requestType.equals(RequestType.GET_CONFIGURATION)) {
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
				request.assignError(property.name()+" is not a property that I can look up");
				break;
			}
			request.setProperty(BottleConstants.TEXT, text);
		}
		else {
			LOGGER.warning(String.format("%s.simulateResponseForRequest: Request type %s not handled",CLSS,requestType));
		}
		return request;
	}
}
