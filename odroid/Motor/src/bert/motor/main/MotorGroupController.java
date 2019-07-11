/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import bert.motor.model.RobotMotorModel;
import bert.share.common.DynamixelType;
import bert.share.controller.Controller;
import bert.share.message.BottleConstants;
import bert.share.message.MessageBottle;
import bert.share.message.MessageHandler;
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
	private MessageBottle currentRequest;
	private final boolean development;
	private int motorCount;
	private int motorsProcessed;
	private MessageHandler responseHandler = null;  // Dispatcher

	private final Map<String,String> parametersInProcess;
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public MotorGroupController(RobotMotorModel m) {
		this.model = m;
		this.motorControllers = new HashMap<>();
		this.motorControllerThreads = new HashMap<>();
		this.currentRequest = null;
		this.motorCount = 0;
		this.motorsProcessed = 0;
		this.motorNameById = new HashMap<>();
		this.parametersInProcess = new HashMap<>();
		LOGGER.info(String.format("%s: os.arch = %s",CLSS,System.getProperty("os.arch")));  // x86_64
		LOGGER.info(String.format("%s: os.name = %s",CLSS,System.getProperty("os.name")));  // Mac OS X
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
			Map<Joint,MotorConfiguration> motors = model.getMotors(); 
			for( String group:groupNames ) {
				SerialPort port = model.getPortForGroup(group);
				MotorController controller = new MotorController(group,port,this);
				Thread t = new Thread(controller);
				motorControllers.put(group, controller);
				motorControllerThreads.put(group, t);
				

				// Add configurations to the controller for each motor in the group
				List<Joint> joints = model.getJointsForGroup(group);
				for( Joint joint:joints ) {
					MotorConfiguration motor = motors.get(joint);
					if( motor!=null ) {
						//LOGGER.info(String.format("%s.initialize: Added motor %s to group %s",CLSS,joint.name(),controller.getGroupName()));
						controller.putMotorConfiguration(joint.name(), motor);
						motorNameById.put(motor.getId(), joint.name());
					}
					else {
						LOGGER.warning(String.format("%s.initialize: Motor %s not found in %s",CLSS,joint.name(),group));
					}
				}
				motorCount += controller.getMotorCount();
				LOGGER.info(String.format("%s.initialize: Created motor controller for group %s, %d motors",CLSS,controller.getGroupName(),controller.getMotorCount()));
			}
		}
	}
	
	@Override
	public int getControllerCount() { return motorControllers.size(); }
	
	public void setResponseHandler(MessageHandler mh) { this.responseHandler = mh; }

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
	 * Called by the Dispatcher when confronted with Motor requests.
	 * There are 3 kinds of messages that we process:
	 * 	1) Requests that can be satisfied from information in our static
	 *     configuration. Compose results and return immediately. 
	 *  2) Commands or requests that can be satisfied by a single port handler,
	 *     for example requesting status or commanding control of a single joint.
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
	public void processRequest(MessageBottle request) {
		if( canHandleImmediately(request) ) {
			responseHandler.handleResponse(createResponseForLocalRequest(request));
		}
		else if(development ) {
			responseHandler.handleResponse(simulateResponseForRequest(request));
		}
		else {
			this.currentRequest = request;
			motorsProcessed = 0;
			parametersInProcess.clear();
			
			LOGGER.info(String.format("%s.processRequest: processing %s",CLSS,request.fetchRequestType().name()));
			for( String key:motorControllers.keySet()) {
				motorControllers.get(key).receiveRequest(request);
			}	
		}
	}
	
	@Override
	public void receiveRequest(MessageBottle request) {}
	@Override
	public void receiveResponse(MessageBottle response) {}
	// =========================== Motor Manager Interface ====================================
	/**
	 * Update the property map with the response from a single serial controller. 
	 * We assume that all controllers have analyzed for the same property. When we have heard
	 * from all the controllers, forward reply back to the Dispatcher.
	 * @param map position of an individual motor.
	 */
	public synchronized void aggregateMotorProperties(Map<Integer,String> map) {
		if( map!=null ) {
			for( Integer key:map.keySet() ) {
				String param = map.get(key);
				String name = motorNameById.get(key);
				parametersInProcess.put(name, param);
				motorsProcessed++;   // Assume there are no duplicates
				LOGGER.info(String.format("%s.aggregateMotorProperties: received %s (%d of %d) = %s",CLSS,name,motorsProcessed,motorCount,param));
			}
		}
		if(motorsProcessed>=motorCount ) {
			LOGGER.info(String.format("%s.aggregateMotorProperties: all motors accounted for: responding ...",CLSS));
			this.currentRequest.setJointValues(parametersInProcess);
			responseHandler.handleResponse(currentRequest);
		}
	}
	/**
	 * This method is called by the controller that handled a request that does
	 * not generate a response. The only information we return is the max 
	 * duration of all the movements.
	 * @param count number of motors represented by this controller
	 * @param duration calculated maximum movement time
	 */
	public synchronized void handleSynthesizedResponse(int count,long duration) {
		motorsProcessed += count;
		LOGGER.info(String.format("%s.handleSynthesizedResponse: received %s (%d of %d)",CLSS,currentRequest.fetchRequestType().name(),motorsProcessed,motorCount));
		if( duration>currentRequest.getDuration() ) currentRequest.setDuration(duration);
		
		if(motorsProcessed>=motorCount ) {
			LOGGER.info(String.format("%s.handleSynthesizedResponse: all motors accounted for: responding ...",CLSS));
			responseHandler.handleResponse(currentRequest);	
		}
	}


	/**
	 * This method is called by the controller that handled a request that pertained to
	 * a single motor. Forward result to the Dispatcher.
	 * @param props the properties modified by or read by the controller
	 */
	public void handleUpdatedProperties(Map<String,String> props) {
		if( props!=null ) {
			for(Object key:props.keySet()) {
				String name = key.toString();
				this.currentRequest.setProperty(name, props.get(key));
			}
		}
		responseHandler.handleResponse(currentRequest);		
	}

	// =========================== Private Helper Methods =====================================
	// Queries of fixed properties of the motors are the kinds of requests that can be handled
	// immediately. Results are created from the original configuration file
	private boolean canHandleImmediately(MessageBottle request) {
		if( request.fetchRequestType().equals(RequestType.GET_MOTOR_PROPERTY) ||
			request.fetchRequestType().equals(RequestType.LIST_MOTOR_PROPERTY) ) {
			// Certain properties are constants available from the configuration file.
			String property = request.getProperty(BottleConstants.PROPERTY_NAME,"");
			if( property.equalsIgnoreCase(JointProperty.ID.name()) ||
				property.equalsIgnoreCase(JointProperty.MINIMUMANGLE.name()) ||
				property.equalsIgnoreCase(JointProperty.MAXIMUMANGLE.name()) ||
				property.equalsIgnoreCase(JointProperty.MOTORTYPE.name()) ||
				property.equalsIgnoreCase(JointProperty.OFFSET.name()) ||
				property.equalsIgnoreCase(JointProperty.ORIENTATION.name()) ) {
				return true;
			}
		}
		else if( request.fetchRequestType().equals(RequestType.SET_MOTOR_PROPERTY)) {
			// Some properties cannot be set. Catch them here in order to formulate an error response.
			String property = request.getProperty(BottleConstants.PROPERTY_NAME,"");
			if( property.equalsIgnoreCase(JointProperty.ID.name()) ||
				property.equalsIgnoreCase(JointProperty.MOTORTYPE.name()) ||
				property.equalsIgnoreCase(JointProperty.OFFSET.name()) ||
				property.equalsIgnoreCase(JointProperty.ORIENTATION.name()) ) {
				return true;
			}
		}
		else if( request.fetchRequestType().equals(RequestType.GET_CONFIGURATION)) {
			return true;
		}
		return false;
	}

	// The "local" response is simply the original request with some text
	// to return directly to the user. These jointValues are obtained from the initial configuration.
	private MessageBottle createResponseForLocalRequest(MessageBottle request) {
		if( request.fetchRequestType().equals(RequestType.GET_MOTOR_PROPERTY)) {
			JointProperty property = JointProperty.valueOf(request.getProperty(BottleConstants.PROPERTY_NAME, ""));
			Joint joint = Joint.valueOf(request.getProperty(BottleConstants.JOINT_NAME, "UNKNOWN"));
			LOGGER.info(String.format("%s.createResponseForLocalRequest: %s %s in %s",CLSS,request.fetchRequestType().name(),property.name(),joint.name()));
			String text = "";
			String jointName = Joint.toText(joint);
			MotorConfiguration mc = model.getMotors().get(joint.name());
			if( mc!=null ) {
				switch(property) {
				case ID:
					int id = mc.getId();
					text = "The id of my "+jointName+" is "+id;
					break;
				case MAXIMUMANGLE:
					text = String.format("The maximum angle of my %s is %.0f degrees",jointName,mc.getMaxAngle());
					break;
				case MINIMUMANGLE:
					text = String.format("The minimum angle of my %s is %.0f degrees",jointName,mc.getMinAngle());
					break;
				case MOTORTYPE:
					String modelName = "A X 12";
					if( mc.getType().equals(DynamixelType.MX28)) modelName = "M X 28";
					else if( mc.getType().equals(DynamixelType.MX64)) modelName = "M X 64";
					text = "My "+jointName+" is a dynamixel "+modelName;
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
			}
			else {
				request.assignError(String.format("The configuration file does not include joint %s", joint.name()));
			}
			request.setProperty(BottleConstants.TEXT, text);
		}
		// Log configuration metrics. The response will contain a map of motor types by ID 
		else if( request.fetchRequestType().equals(RequestType.GET_CONFIGURATION)) {
			String text = "Motor configuration parameters have been logged";
			request.setProperty(BottleConstants.TEXT, text);
			for( String group:motorControllers.keySet() ) {
				MotorController controller = motorControllers.get(group);
				Map<String,MotorConfiguration> map = controller.getConfigurations();
				for(String joint:map.keySet()) {
					MotorConfiguration mc = map.get(joint);
					LOGGER.info(String.format("Joint: %s (%d) %s min,max,offset = %f.0 %f.0 %f.0 %s",joint,mc.getId(),mc.getType().name(),
									mc.getMinAngle(),mc.getMaxAngle(),mc.getOffset(),(mc.isDirect()?"":"(indirect)") ));
					request.setProperty(BottleConstants.PROPERTY_NAME, JointProperty.MOTORTYPE.name());
					request.setJointValue(JointProperty.MOTORTYPE.name(), mc.getType().name());
				}
			}
		}
		else if( request.fetchRequestType().equals(RequestType.LIST_MOTOR_PROPERTY)) {
			JointProperty property = JointProperty.valueOf(request.getProperty(BottleConstants.PROPERTY_NAME, ""));
			LOGGER.info(String.format("%s.createResponseForLocalRequest: %s %s for all motors",CLSS,request.fetchRequestType().name(),property.name()));
			String text = "";
			Map<Joint,MotorConfiguration> mcs = model.getMotors();
			for( Joint joint:mcs.keySet() ) {
				MotorConfiguration mc = mcs.get(joint);
				switch(property) {
				case ID:
					int id = mc.getId();
					text = "The id of "+joint+" is "+id;
					break;
				case MAXIMUMANGLE:
					text = String.format("The maximum angle for %s is %.0f degrees",joint,mc.getMaxAngle());
					break;
				case MINIMUMANGLE:
					text = String.format("The minimum angle for %s is %.0f degrees",joint,mc.getMinAngle());
					break;
				case MOTORTYPE:
					String modelName = "A X 12";
					if( mc.getType().equals(DynamixelType.MX28)) modelName = "M X 28";
					else if( mc.getType().equals(DynamixelType.MX64)) modelName = "M X 64";
					text = joint+" is a dynamixel "+modelName;
					break;
				case OFFSET:
					double offset = mc.getOffset();
					text = "The offset of "+joint+" is "+offset;
					break;
				case ORIENTATION:
					String orientation = "indirect";
					if( mc.isDirect() ) orientation = "direct";
					text = "The orientation of "+joint+" is "+orientation;
					break;
				default:
					text = "";
					request.assignError(property.name()+" is not a property that I can look up");
					break;
				}
				LOGGER.info(text);
			}
			text = String.format("The %ss of all motors have been logged",property.name().toLowerCase());
			request.setProperty(BottleConstants.TEXT, text);
		}
		else if( request.fetchRequestType().equals(RequestType.SET_MOTOR_PROPERTY)) {
			String property = request.getProperty(BottleConstants.PROPERTY_NAME,"");
			request.assignError("I cannot change a motor "+property.toLowerCase());
		}
		return request;
	}
	
	// When in development mode, simulate something reasonable as a response.
	private MessageBottle simulateResponseForRequest(MessageBottle request) {
		RequestType requestType = request.fetchRequestType();
		if( requestType.equals(RequestType.GET_MOTOR_PROPERTY)) {
			JointProperty property = JointProperty.valueOf(request.getProperty(BottleConstants.PROPERTY_NAME, ""));
			Joint joint = Joint.valueOf(request.getProperty(BottleConstants.JOINT_NAME, "UNKNOWN"));
			String text = "";
			String jointName = Joint.toText(joint);
			MotorConfiguration mc = model.getMotors().get(joint);
			switch(property) {
			case POSITION:
				int position = 0;
				text = "The position of my "+jointName+" is "+position;
				break;
			default:
				text = "";
				request.assignError(property.name()+" is not a property that I can read");
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
