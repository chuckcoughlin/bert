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
public class MotorGroupController implements MotorManager {
	private final static String CLSS = "MotorGroupController";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private final RobotMotorModel model;
	private final Map<String,MotorController> motorControllers;
	private final Map<Integer,String> motorNameById;
	private final Map<String,Thread> motorControllerThreads;
	private final boolean development;
	private int controllerCount;
	private MessageHandler responseHandler = null;  // Dispatcher
	/**
	 * Constructor:
	 * @param m the server model
	 */
	public MotorGroupController(RobotMotorModel m) {
		this.model = m;
		this.controllerCount = 0;
		this.motorControllers = new HashMap<>();
		this.motorControllerThreads = new HashMap<>();
		this.motorNameById = new HashMap<>();
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
			Set<String> controllerNames = model.getHandlerTypes().keySet();
			Map<Joint,MotorConfiguration> motors = model.getMotors(); 
			for( String cname:controllerNames ) {
				SerialPort port = model.getPortForController(cname);
				MotorController controller = new MotorController(cname,port,this);
				Thread t = new Thread(controller);
				motorControllers.put(cname, controller);
				motorControllerThreads.put(cname, t);
				

				// Add configurations to the controller for each motor in the group
				List<Joint> joints = model.getJointsForController(cname);
				for( Joint joint:joints ) {
					MotorConfiguration motor = motors.get(joint);
					if( motor!=null ) {
						//LOGGER.info(String.format("%s.initialize: Added motor %s to group %s",CLSS,joint.name(),controller.getGroupName()));
						controller.putMotorConfiguration(joint.name(), motor);
						motorNameById.put(motor.getId(), joint.name());
					}
					else {
						LOGGER.warning(String.format("%s.initialize: Motor %s not found in %s",CLSS,joint.name(),cname));
					}
				}
				controllerCount += 1;
				LOGGER.info(String.format("%s.initialize: Created motor controller for group %s",CLSS,controller.getGroupName()));
			}
		}
	}
	
	@Override
	public int getControllerCount() { return motorControllers.size(); }
	
	public void setResponseHandler(MessageHandler mh) { this.responseHandler = mh; }

	public void start() {
		if(!development ) {
			for( String key:motorControllers.keySet()) {
				MotorController controller = motorControllers.get(key);
				controller.setStopped(false);
				controller.initialize();
				motorControllerThreads.get(key).start();
			}
		}
	}

	/** 
	 * Called by Dispatcher when it is stopped.
	 */
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
	 *     These requests are satisfied by sending the same request to all port handlers.
	 *     The single unique request object collects partial results from all
	 *     controllers. When complete, it is passed here and forwarded to the dispatcher.
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
			LOGGER.info(String.format("%s.processRequest: processing %s",CLSS,request.fetchRequestType().name()));
			for( MotorController controller:motorControllers.values()) {
				controller.receiveRequest(request);
			}	
		}
	}
	
	// =========================== Motor Manager Interface ====================================
	/**
	 * When the one of the controllers has detected the response is complete, it calls
	 * this method. When all controllers have responded the response will be sent off.
	 * For this to work, it is important that work on the response object
	 * by synchronized.
	 * @param rsp the original request
	 */
	public void handleAggregatedResponse(MessageBottle rsp) {
		int count = rsp.incrementResponderCount();
		LOGGER.info(String.format("%s.handleAggregatedResponse: received %s (%d of %d)",CLSS,rsp.fetchRequestType().name(),count,controllerCount));
		
		if(count>=controllerCount ) {
			LOGGER.info(String.format("%s.handleAggregatedResponse: all controllers accounted for: responding ...",CLSS));
			responseHandler.handleResponse(rsp);	
		}
	}
	
	/**
	 * This method is called by each controller as it handles a request that does
	 * not generate a response. Once each controller has responded, we forward the
	 * result to the dispatcher.
	 * @param rsp the response
	 */
	public synchronized void handleSynthesizedResponse(MessageBottle rsp) {
		int count = rsp.incrementResponderCount();
		LOGGER.info(String.format("%s.handleSynthesizedResponse: received %s (%d of %d)",CLSS,rsp.fetchRequestType().name(),count,controllerCount));
		
		if(count>=controllerCount ) {
			LOGGER.info(String.format("%s.handleSynthesizedResponse: all controllers accounted for: responding ...",CLSS));
			responseHandler.handleResponse(rsp);	
		}
	}


	/**
	 * This method is called by the controller that handled a request that pertained to
	 * a single motor. It has modified the request directly. Forward result to the Dispatcher.
	 * @param response the message to be forwarded.
	 */
	public void handleSingleMotorResponse(MessageBottle response) {
		responseHandler.handleResponse(response);		
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
		else if( request.fetchRequestType().equals(RequestType.SET_LIMB_PROPERTY)) {
			// Some properties cannot be set. Catch them here in order to formulate an error response.
			String property = request.getProperty(BottleConstants.PROPERTY_NAME,"");
			if( property.equalsIgnoreCase(JointProperty.ID.name()) ||
				property.equalsIgnoreCase(JointProperty.MOTORTYPE.name()) ||
				property.equalsIgnoreCase(JointProperty.OFFSET.name()) ||
				property.equalsIgnoreCase(JointProperty.ORIENTATION.name()) ||
				property.equalsIgnoreCase(JointProperty.POSITION.name())) {
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
			MotorConfiguration mc = model.getMotors().get(joint);
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
		else if( request.fetchRequestType().equals(RequestType.SET_LIMB_PROPERTY)) {
			String property = request.getProperty(BottleConstants.PROPERTY_NAME,"");
			request.assignError("I cannot change "+property.toLowerCase()+" for all joints in the limb");
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
