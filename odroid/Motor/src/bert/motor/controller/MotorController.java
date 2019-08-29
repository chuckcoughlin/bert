/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.motor.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import bert.motor.dynamixel.DxlMessage;
import bert.share.common.BottleConstants;
import bert.share.message.MessageBottle;
import bert.share.message.RequestType;
import bert.share.model.Joint;
import bert.share.model.JointProperty;
import bert.share.model.Limb;
import bert.share.model.MotorConfiguration;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 *  Handle requests directed to a specific controllerName of motors. All motors under the 
 *  same controller are connected to the same serial port. We respond to the group controller
 *  using call-backs. The responses from the serial port do not necessarily keep
 *  to request boundaries. All we are sure of is that the requests are processed
 *  in order. 
 *  
 *  The configuration array has only those joints that are part of the controllerName.
 *  It is important that the MotorConfiguration objects are the same objects
 *  (not clones) as those held by the MotorManager (MotorGroupController).
 */
public class MotorController implements  Runnable, SerialPortEventListener {
	protected static final String CLSS = "MotorController";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static final int BAUD_RATE = 1000000;
	private static final int MIN_WRITE_INTERVAL = 100; // msecs between writes (50 was too short)
	private static final int STATUS_RESPONSE_LENGTH = 8; // byte count
	private final Condition running;
	private final DxlMessage dxl;
	private final String controllerName;                 // Group name
	private final Lock lock;
	private final SerialPort port;
	private boolean stopped = false;
	private final MotorManager motorManager;
	private final Map<Integer,MotorConfiguration> configurationsById;
	private final Map<String,MotorConfiguration> configurationsByName;
	private byte[] remainder = null;
	private final LinkedList<MessageBottle> requestQueue;   // requests waiting to be processed
	private final LinkedList<MessageWrapper> responseQueue; // responses waiting for serial results
	private long timeOfLastWrite;

	public MotorController(String name,SerialPort p,MotorManager mm) {
		this.dxl = new DxlMessage();
		this.controllerName = name;
		this.port = p;
		this.motorManager = mm;
		this.configurationsById = new HashMap<>();
		this.configurationsByName = new HashMap<>();
		this.requestQueue = new LinkedList<>();
		this.responseQueue = new LinkedList<>();
		this.lock = new ReentrantLock();
		this.running = lock.newCondition();
		this.timeOfLastWrite = System.nanoTime()/1000000; 
	}

	public String getControllerName() { return this.controllerName; }
	public Map<String,MotorConfiguration> getConfigurations() { return this.configurationsByName; }
	public MotorConfiguration getMotorConfiguration(String name) { return configurationsByName.get(name); }
	public void putMotorConfiguration(String name,MotorConfiguration mc) {
		configurationsById.put(mc.getId(), mc);
		configurationsByName.put(name, mc);
	}

	/**
	 * Open and configure the port.
	 * Dynamixel documentation: No parity, 1 stop bit, 8 bits of data, no flow control
	 * 
	 * At one point, we thought we should initialize the motors somehow.  This is now
	 * taken care of by the dispatcher. The dispatcher:
	 * 	1) requests a list of current positions (thus updating the MotorConfigurations)
	 * 	2) sets travel speeds to "normal"
	 * 	3) moves any limbs that are "out-of-bounds" back into range.
	 */
	public void initialize() {
		LOGGER.info(String.format("%s(%s).initialize: Initializing port %s)",CLSS,controllerName,port.getPortName()));
		if( !port.isOpened()) {
			try {
				boolean success = port.openPort();
				if( success && port.isOpened()) {
					port.setParams(BAUD_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);  
					port.setEventsMask(SerialPort.MASK_RXCHAR);
					port.purgePort(SerialPort.PURGE_RXCLEAR);
					port.purgePort(SerialPort.PURGE_TXCLEAR);
					port.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT);
					port.addEventListener(this);
				}
				else {
					LOGGER.severe(String.format("%s.initialize: Failed to open port %s for %s",CLSS,port.getPortName(),controllerName));
				}
			}
			catch(SerialPortException spe) {
				LOGGER.severe(String.format("%s.initialize: Error opening port %s for %s (%s)",CLSS,port.getPortName(),controllerName,spe.getLocalizedMessage()));
			}
			LOGGER.info(String.format("%s.initialize: Initialized port %s)",CLSS,port.getPortName()));
		}
	}
	public void stop() {
		try {
			port.closePort();
		}
		catch(SerialPortException spe) {
			LOGGER.severe(String.format("%s.close: Error closing port for %s (%s)",CLSS,controllerName,spe.getLocalizedMessage()));
		}
		stopped = true;
	}
	public void setStopped(boolean flag) { this.stopped = flag; }
	

	/**
	 * This method blocks until the prior request completes. Ignore requests that apply to a single controller
	 * and that controller is not this one, otherwise add the request to the request queue. 
	 * @param request
	 */
	public void receiveRequest(MessageBottle request) {
		lock.lock();
		//LOGGER.info(String.format("%s(%s).receiveRequest: processing %s",CLSS,controllerName,request.fetchRequestType().name()));
		try {
			if( isLocalRequest(request) ) {
				handleLocalRequest(request);
				return;
			}
			else if( isSingleControllerRequest(request)) {
				// Do nothing if the joint or limb isn't in our controllerName.
				String jointName = request.getProperty(BottleConstants.JOINT_NAME,Joint.UNKNOWN.name());
				String cName = request.getProperty(BottleConstants.CONTROLLER_NAME,"");
				String limbName = request.getProperty(BottleConstants.LIMB_NAME,Limb.UNKNOWN.name());
				if( !jointName.equalsIgnoreCase(Joint.UNKNOWN.name())) {
					MotorConfiguration mc = configurationsByName.get(jointName);
					if( mc==null ) { 
						return; 
					}
				}
				else if(!cName.isEmpty()) {
					if( !cName.equalsIgnoreCase(controllerName) ) {
						return;
					}
				}
				else if(!limbName.equalsIgnoreCase(Limb.UNKNOWN.name())) {
					Limb limb = Limb.valueOf(limbName);
					int count = configurationsForLimb(limb).size();
					if( count==0 ) {
						return;
					}
				}
				else {
					String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME,JointProperty.UNRECOGNIZED.name());
					LOGGER.info(String.format("%s(%s).receiveRequest: %s (%s)",CLSS,controllerName,request.fetchRequestType().name(),propertyName));
				}
			}
			else {
				LOGGER.info(String.format("%s(%s).receiveRequest: multi-controller request (%s)",CLSS,controllerName,request.fetchRequestType().name()));
			}
			requestQueue.addLast(request);
			// LOGGER.info(String.format("%s(%s).receiveRequest: added to request queue %s",CLSS,controllerName,request.fetchRequestType().name()));
			running.signal();
		}
		finally {
			lock.unlock();
		}
	}
	
		
	
	/**
	 *  Wait until we receive a request message. Convert to serial request, write to port.
	 *  From there a listener forwards the responses to the controllerName controller (MotorManager).
	 *  Do not free the request lock until we have the response in-hand.
	 *  
	 *  Integer.toHexString(this.hashCode()) 
	 */
	@Override
	public void run() {
		while( !stopped ) {
			lock.lock();
			try{
				running.await();
				// LOGGER.info(String.format("%s.run: %s Got signal for message, writing to %s",CLSS,controllerName,port.getPortName()));
				MessageBottle req = requestQueue.removeFirst();  // Oldest
				MessageWrapper wrapper = new MessageWrapper(req);
				if(isSingleWriteRequest(req) ) {
					byte[] bytes = messageToBytes(wrapper);
					if( bytes!=null ) {
						if( wrapper.getResponseCount()>0) {
							responseQueue.addLast(wrapper);
						}
						writeBytesToSerial(bytes);
						LOGGER.info(String.format("%s(%s).run: wrote %d bytes",CLSS,controllerName,bytes.length));
					}
				}
				else  {
					List<byte[]> byteArrayList = messageToByteList(wrapper);
					if( wrapper.getResponseCount()>0) {
						responseQueue.addLast(wrapper);
					}
					for(byte[] bytes:byteArrayList) {
						writeBytesToSerial(bytes);
						LOGGER.info(String.format("%s(%s).run: wrote %d bytes",CLSS,controllerName,bytes.length));
					}
				}
				
				if( wrapper.getResponseCount()==0) {
					synthesizeResponse(req);
				}
			}
			catch(InterruptedException ie ) {}
			finally {
				lock.unlock();
			}
		}
	}
	
	
	// ============================= Private Helper Methods =============================

	// Create a response for a request that can be handled immediately. There aren't many of them. The response is simply the original request
	// with some text to send directly to the user. 
	private MessageBottle handleLocalRequest(MessageBottle request) {
		// The following two requests simply use the current positions of the motors, whatever they are
		if(request.fetchRequestType().equals(RequestType.COMMAND)) {
			String command = request.getProperty(BottleConstants.COMMAND_NAME, "NONE");
			LOGGER.warning(String.format("%s(%s).createResponseForLocalRequest: command=%s",CLSS,controllerName,command));
			if( command.equalsIgnoreCase(BottleConstants.COMMAND_RESET)) {
				remainder = null;   // Resync after dropped messages.
				responseQueue.clear();
        		motorManager.handleAggregatedResponse(request);
			}
			else {
				String msg = String.format("Unrecognized command: %s",command);
				request.assignError(msg);
			}
		}
		return request;
	}
	/**
	 * @param msg the request
	 * @return true if this is the type of request that can be satisfied locally.
	 */
	private boolean isLocalRequest(MessageBottle msg) {
		if( msg.fetchRequestType().equals(RequestType.COMMAND) &&
			msg.getProperty(BottleConstants.COMMAND_NAME, "NONE").equalsIgnoreCase(BottleConstants.COMMAND_RESET) ) {
			return true;
		}
		return false;
	}
	/**
	 * @param msg the request
	 * @return true if this is the type of request satisfied by a single controller.
	 */
	private boolean isSingleControllerRequest(MessageBottle msg) {
		if( msg.fetchRequestType().equals(RequestType.GET_GOALS) 		 ||
			msg.fetchRequestType().equals(RequestType.GET_LIMITS)  		 ||
			msg.fetchRequestType().equals(RequestType.GET_MOTOR_PROPERTY)||
			msg.fetchRequestType().equals(RequestType.SET_LIMB_PROPERTY) ||
			msg.fetchRequestType().equals(RequestType.SET_MOTOR_PROPERTY)  ){
			return true;
		}
		// LIST_MOTOR_PROPERTY applies to a single controller if controller name or limb is specified
		else if( msg.fetchRequestType().equals(RequestType.LIST_MOTOR_PROPERTY) &&
				(!msg.getProperty(BottleConstants.CONTROLLER_NAME, "").equals( "") ||
				 !msg.getProperty(BottleConstants.LIMB_NAME, Limb.UNKNOWN.name()).equalsIgnoreCase( Limb.UNKNOWN.name())) ) {
			return true;
		}
		return false;
	}
	
	/**
	 * The list here should match the request types in messageToByteList().
	 * @param msg the request
	 * @return true if this request translates into a single serial message.
	 *         false implies that an array of serial messages are required.
	 */
	private boolean isSingleWriteRequest(MessageBottle msg) {
		if( msg.fetchRequestType().equals(RequestType.INITIALIZE_JOINTS)   ||
			msg.fetchRequestType().equals(RequestType.LIST_MOTOR_PROPERTY) ||
			msg.fetchRequestType().equals(RequestType.SET_POSE))    {
			return false;
		}
		return true;
	}
	/**
	 * @param msg the request
	 * @return true if this is the type of message that returns a separate
	 *         status response for every motor referenced in the request.
	 *         (There may be only one).
	 */
	private boolean returnsStatusArray(MessageBottle msg) {
		if( msg.fetchRequestType().equals(RequestType.GET_MOTOR_PROPERTY) ||
			msg.fetchRequestType().equals(RequestType.LIST_MOTOR_PROPERTY) 	)    {
			return true;
		}
		return false;
	}

	/**
	 * Convert the request message into a command for the serial port. As a side
	 * effect set the number of expected responses. This can vary by request type.
	 * @param wrapper
	 * @return
	 */
	private byte[] messageToBytes(MessageWrapper wrapper) {
		MessageBottle request = wrapper.getMessage();
		byte[] bytes = null;
		if( request!=null) {
			RequestType type = request.fetchRequestType();
			if( type.equals(RequestType.COMMAND) && 
					request.getProperty(BottleConstants.COMMAND_NAME, "").equalsIgnoreCase(BottleConstants.COMMAND_FREEZE) ) {
				String propertyName = JointProperty.STATE.name();
				for(MotorConfiguration mc:configurationsByName.values()) {
					mc.setTorqueEnabled(true);
				}
				bytes = dxl.byteArrayToSetProperty(configurationsByName,propertyName);
				wrapper.setResponseCount(0);  // No response
			}
			else if( type.equals(RequestType.COMMAND) && 
					request.getProperty(BottleConstants.COMMAND_NAME, "").equalsIgnoreCase(BottleConstants.COMMAND_RELAX) ) {
				for(MotorConfiguration mc:configurationsByName.values()) {
					mc.setTorqueEnabled(false);
				}
				String propertyName = JointProperty.STATE.name();
				bytes = dxl.byteArrayToSetProperty(configurationsByName,propertyName);
				wrapper.setResponseCount(0);  // No response
			}
			else if( type.equals(RequestType.GET_GOALS)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				MotorConfiguration mc = configurationsByName.get(jointName);
				if( mc!=null) {
					bytes = dxl.bytesToGetGoals(mc.getId());
					wrapper.setResponseCount(1);   // Status message
				}
			}
			else if( type.equals(RequestType.GET_LIMITS)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				MotorConfiguration mc = configurationsByName.get(jointName);
				if( mc!=null ) {
					bytes = dxl.bytesToGetLimits(mc.getId());
					wrapper.setResponseCount(1);   // Status message
				}
			}
			else if( type.equals(RequestType.GET_MOTOR_PROPERTY)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				MotorConfiguration mc = configurationsByName.get(jointName);
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, "");
				if( mc!=null ) {
					bytes = dxl.bytesToGetProperty(mc.getId(),propertyName);
					wrapper.setResponseCount(1);   // Status message
				}
			}
			else if( type.equals(RequestType.SET_LIMB_PROPERTY)) {
				String limbName = request.getProperty(BottleConstants.LIMB_NAME, Limb.UNKNOWN.name());
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, JointProperty.UNRECOGNIZED.name());
				JointProperty jp = JointProperty.valueOf(propertyName);
				double value = Double.parseDouble(request.getProperty(propertyName.toUpperCase(),"0.0"));
				// Loop over motor config map, set the property
				Limb limb = Limb.valueOf(limbName);
				Map<String,MotorConfiguration> configs = configurationsForLimb(limb);
				for(MotorConfiguration mc:configs.values()) {
					mc.setProperty(jp, value);
				}
				bytes = dxl.byteArrayToSetProperty(configs,propertyName);  // Returns null if limb not on this controller
				wrapper.setResponseCount(0);  // ASYNC WRITE, no response. Let source set text.
			}
			else if( type.equals(RequestType.SET_MOTOR_PROPERTY)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, Joint.UNKNOWN.name());
				MotorConfiguration mc = configurationsByName.get(jointName);
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, "");
				String value = request.getProperty(propertyName.toUpperCase(),"0.0");
				if( value!=null && !value.isEmpty() && mc!=null ) {
					bytes = dxl.bytesToSetProperty(mc,propertyName,Double.parseDouble(value));
					if(propertyName.equalsIgnoreCase("POSITION")) {
						long duration = mc.getTravelTime();
						if(request.getDuration()<duration) request.setDuration(duration);
						request.assignText(String.format("My position is %.0f", mc.getPosition()));
					}
					else if(propertyName.equalsIgnoreCase("STATE")) {
						request.assignText(String.format("My %s state is torque-%s",Joint.toText(mc.getJoint()),
								(value.equalsIgnoreCase("0")?"disabled":"enabled")));
					}
					else {
						request.assignText(String.format("My %s %s is %s",Joint.toText(mc.getJoint()),propertyName.toLowerCase(),value));
					}
					wrapper.setResponseCount(1);   // Status message
				}
				else {
					LOGGER.warning(String.format("%s.messageToBytes: Empty property value - ignored (%s)",CLSS,type.name()));
					wrapper.setResponseCount(0);   // Error, there will be no response
				}
			}
			else if( type.equals(RequestType.NONE)) {
				LOGGER.warning(String.format("%s.messageToBytes: Empty request - ignored (%s)",CLSS,type.name()));
				wrapper.setResponseCount(0);   // Error, there will be no response
			}
			else {
				LOGGER.severe(String.format("%s.messageToBytes: Unhandled request type %s",CLSS,type.name()));
				wrapper.setResponseCount(0);   // Error, there will be no response
			}
			LOGGER.info(String.format("%s.messageToBytes: %s = \n%s",CLSS,request.fetchRequestType(),dxl.dump(bytes)));
		}
		return bytes;
	}
	
	/**
	 * Convert the request message into a list of commands for the serial port. As a side
	 * effect set the number of expected responses. This can vary by request type (and may 
	 * be none).
	 * @param wrapper
	 * @return
	 */
	private List<byte[]> messageToByteList(MessageWrapper wrapper) {
		MessageBottle request = wrapper.getMessage();
		List<byte[]> list = new ArrayList<>();
		if( request!=null) {
			RequestType type = request.fetchRequestType();
			// Unfortunately broadcast requests don't work here. We have to concatenate the
			// requests into single long lists.
			if( type.equals(RequestType.INITIALIZE_JOINTS)) {
				list = dxl.byteArrayListToInitializePositions(configurationsByName.values());
				long duration = dxl.getMostRecentTravelTime();
				if( request.getDuration()<duration ) request.setDuration(duration);
				wrapper.setResponseCount(0);  // No response
			}
			else if( type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
				String limbName = request.getProperty(BottleConstants.LIMB_NAME, Limb.UNKNOWN.name());
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, "");
				if( limbName.equalsIgnoreCase(Limb.UNKNOWN.name())) {
					list = dxl.byteArrayListToListProperty(propertyName,configurationsByName.values());
					wrapper.setResponseCount(configurationsByName.size());  // Status packet for each motor
				}
				// A specific limb. Configuration array contains only those limbs. (May be empty)
				else {
					Limb limb = Limb.valueOf(limbName);
					Map<String,MotorConfiguration> configs = configurationsForLimb(limb);
					list = dxl.byteArrayListToListProperty(propertyName,configs.values());
					wrapper.setResponseCount(configs.size());  // Status packet for each motor in limb
				}
			}
			else if( type.equals(RequestType.SET_POSE)) {
				String poseName = request.getProperty(BottleConstants.POSE_NAME, "");
				list = dxl.byteArrayListToSetPose(configurationsByName,poseName);
				long duration = dxl.getMostRecentTravelTime();
				if( request.getDuration()<duration ) request.setDuration(duration);
				wrapper.setResponseCount(0);  // AYNC WRITE, no responses
			}
			else {
				LOGGER.severe(String.format("%s.messageToByteList: Unhandled request type %s",CLSS,type.name()));
			}
			for(byte[] bytes:list) {
				LOGGER.info(String.format("%s(%s).messageToByteList: %s = \n%s",CLSS,controllerName,request.fetchRequestType(),dxl.dump(bytes)));
			}
		}
		return list;
	}
	
	/**
	 * We have just written a message to the serial port that generates no
	 * response. Make one up and send it off to the "MotorManager". It expects
	 * a response from each controller.
	 * @param msg the request
	 */
	private void synthesizeResponse(MessageBottle msg) {

		if( msg.fetchRequestType().equals(RequestType.INITIALIZE_JOINTS) ||
			msg.fetchRequestType().equals(RequestType.SET_POSE)	) {
        	motorManager.handleSynthesizedResponse(msg);
        } 	
		else if( msg.fetchRequestType().equals(RequestType.COMMAND) ) {
			String cmd = msg.getProperty(BottleConstants.COMMAND_NAME, "");
			if( cmd.equalsIgnoreCase(BottleConstants.COMMAND_FREEZE) ||
				cmd.equalsIgnoreCase(BottleConstants.COMMAND_RELAX) ) {
				motorManager.handleSynthesizedResponse(msg);
			}
			else {
				LOGGER.severe( String.format("%s.synthesizeResponse: Unhandled response for command %s",CLSS,cmd));
				motorManager.handleSingleControllerResponse(msg);  // Probably an error
			}
		}
		else if( msg.fetchRequestType().equals(RequestType.SET_LIMB_PROPERTY) ) {
	        	motorManager.handleSingleControllerResponse(msg);
	        } 	
		else  {
			LOGGER.severe( String.format("%s.synthesizeResponse: Unhandled response for %s",CLSS,msg.fetchRequestType().name()));
			motorManager.handleSingleControllerResponse(msg);  // Probably an error
        }
	}
	

	// We update the properties in the request from our serial message.
	// The properties must include motor type and orientation
	private void updateRequestFromBytes(MessageBottle request,byte[] bytes) {
		if( request!=null) {
			RequestType type = request.fetchRequestType();
			Map<String,String> properties = request.getProperties();
			if( type.equals(RequestType.GET_GOALS)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, Joint.UNKNOWN.name());
				MotorConfiguration mc = getMotorConfiguration(jointName);
				dxl.updateGoalsFromBytes(mc,properties,bytes);
			} 
			else if( type.equals(RequestType.GET_LIMITS)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, Joint.UNKNOWN.name());
				MotorConfiguration mc = getMotorConfiguration(jointName);
				dxl.updateLimitsFromBytes(mc,properties,bytes);
			} 
			else if( type.equals(RequestType.GET_MOTOR_PROPERTY)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, Joint.UNKNOWN.name());
				MotorConfiguration mc = getMotorConfiguration(jointName);
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, JointProperty.UNRECOGNIZED.name());
				dxl.updateParameterFromBytes(propertyName,mc,properties,bytes);
				String partial = properties.get(BottleConstants.TEXT);
				if( partial!=null && !partial.isEmpty()) {
					Joint joint = Joint.valueOf(jointName);
					properties.put(BottleConstants.TEXT,String.format("My %s %s is %s", Joint.toText(joint),propertyName.toLowerCase(),partial));
				}	
			}
			// The update applies to only one of several motors affected by this request
			else if( type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
				int id = bytes[2];
				MotorConfiguration mc = configurationsById.get(id);
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, JointProperty.UNRECOGNIZED.name());
				dxl.updateParameterFromBytes(propertyName,mc,properties,bytes);	
			} 
			// The only interesting response is the error code.
			else if( type.equals(RequestType.SET_LIMB_PROPERTY) ||
					 type.equals(RequestType.SET_MOTOR_PROPERTY)) {
				String err =  dxl.errorMessageFromStatus(bytes);
				if( err!=null && !err.isEmpty() ) {
					request.assignError(err);
				}
			} 
			else {
				LOGGER.severe( String.format("%s.updateRequestFromBytes: Unhandled response for %s",CLSS,type.name()));
			}
		}
	}
	
	// The bytes array contains the results of a request for status. It may be the concatenation
	// of several responses. Update the loacal motor configuration map and return a map keyed by motor
	// id to be aggregated by the MotorManager with similar responses from other motors.
	// 
	private Map<Integer,String> updateStatusFromBytes(String propertyName,byte[] bytes) {
		Map<Integer,String> props = new HashMap<>();
		dxl.updateParameterArrayFromBytes(propertyName,configurationsById,bytes,props);
		return props;
	}

	/*
	 * Guarantee that consecutive writes won't be closer than MIN_WRITE_INTERVAL
	 */
	private void writeBytesToSerial(byte[] bytes) {
		if( bytes!=null && bytes.length>0 ) {
			try {
				long now = System.nanoTime()/1000000;
				long interval = now - timeOfLastWrite;
				if( interval<MIN_WRITE_INTERVAL) {
					Thread.sleep(MIN_WRITE_INTERVAL-interval);
					//LOGGER.info(String.format("%s(%s).writeBytesToSerial: Slept %d msecs",CLSS,controllerName,MIN_WRITE_INTERVAL-interval));
				}
				
				LOGGER.info(String.format("%s(%s).writeBytesToSerial: Write interval %d msecs",CLSS,controllerName,interval));
				boolean success = port.writeBytes(bytes);
				timeOfLastWrite = System.nanoTime()/1000000;
				port.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);   // Force the write to complete
				if( !success ) {
					LOGGER.severe(String.format("%s(%s).writeBytesToSerial: Failed write of %d bytes to %s",CLSS,controllerName,bytes.length,port.getPortName()));
				}
			}
			catch(InterruptedException ie) {
				LOGGER.severe(String.format("%s(%s).writeBytesToSerial: Interruption writing to %s (%s)",CLSS,controllerName,port.getPortName(),ie.getLocalizedMessage()));
			}
			catch(SerialPortException spe) {
				LOGGER.severe(String.format("%s(%s).writeBytesToSerial: Error writing to %s (%s)",CLSS,controllerName,port.getPortName(),spe.getLocalizedMessage()));
			}
		}
	}
	// ============================== SerialPortEventListener ===============================
	/**
	 * Handle the response from the serial request. Note that all our interactions with removing from the
	 * responseQueue and dealing with remainder are synchronized here. 
	 * 
	 * The response queue must have at least one response for associating results.
	 */
	public synchronized void serialEvent(SerialPortEvent event) {
		LOGGER.info(String.format("%s(%s).serialEvent queue is %d",CLSS,controllerName,responseQueue.size()));
		if(event.isRXCHAR() && !responseQueue.isEmpty() ){
			MessageWrapper wrapper = responseQueue.getFirst();
			MessageBottle req = wrapper.getMessage();
			// The value is the number of bytes in the read buffer
			int byteCount = event.getEventValue();
			LOGGER.info(String.format("%s(%s).serialEvent (%s) %s: expect %d msgs got %d bytes",
					CLSS,controllerName,event.getPortName(),req.fetchRequestType().name(),wrapper.getResponseCount(),byteCount));
			if(byteCount>0){
				try {
					byte[] bytes = port.readBytes(byteCount);
					bytes = prependRemainder(bytes);
					bytes = dxl.ensureLegalStart(bytes);  // null if no start characters
					if( bytes!=null ) {
						int nbytes = bytes.length;
						LOGGER.info(String.format("%s(%s).serialEvent: read =\n%s",CLSS,controllerName,dxl.dump(bytes)));
						int mlen = dxl.getMessageLength(bytes);  // First message
						if( mlen<0 || nbytes < mlen) {
							LOGGER.info(String.format("%s(%s).serialEvent Message too short (%d), requires additional read",CLSS,controllerName,nbytes));
							return;
						}
						if( returnsStatusArray(req) ) {  // Some requests return a message for each motor
							int nmsgs = nbytes/STATUS_RESPONSE_LENGTH;
							if( nmsgs>wrapper.getResponseCount() ) nmsgs = wrapper.getResponseCount();
							nbytes = nmsgs*STATUS_RESPONSE_LENGTH;
							if( nbytes<bytes.length ) {
								bytes = truncateByteArray(bytes,nbytes);
							}
							
							String propertyName = req.getProperty(BottleConstants.PROPERTY_NAME, "NONE");
							Map<Integer,String> map = updateStatusFromBytes(propertyName,bytes);
							for( Integer key:map.keySet() ) {
								String param = map.get(key);
								String name = configurationsById.get(key).getJoint().name();
								req.setJointValue(name, param);
								wrapper.decrementResponseCount();
								LOGGER.info(String.format("%s(%s).serialEvent: received %s (%d remaining) = %s",
										CLSS,controllerName,name,wrapper.getResponseCount(),param));
							}
						}

						if( wrapper.getResponseCount()<=0 ) {
							responseQueue.removeFirst();
							if( isSingleControllerRequest(req)) {
								updateRequestFromBytes(req,bytes);
								motorManager.handleSingleControllerResponse(req);
							}
							else {
								motorManager.handleAggregatedResponse(req);
							}
						}
					}
				}
				catch (SerialPortException ex) {
					System.out.println(ex);
				}
			}
		}
    }
	
	private Map<String,MotorConfiguration> configurationsForLimb(Limb limb) {
		Map<String,MotorConfiguration> result = new HashMap<>();
		for(MotorConfiguration mc:configurationsByName.values()) {
			if( mc.getLimb().equals(limb)) {
				result.put(mc.getJoint().name(), mc);
			}
		}
		return result;
	}
	/**
	 * Combine the remainder from the previous serial read. Set the remainder null.
	 * @param bytes
	 * @return
	 */
	private byte[] prependRemainder(byte[] bytes) {
		if( remainder==null ) return bytes;
		byte[] combination = new byte[remainder.length + bytes.length];
		System.arraycopy(remainder, 0, combination, 0, remainder.length);
		System.arraycopy(bytes, 0, combination, remainder.length, bytes.length);
		remainder = null;
		return combination;
	}
	/**
	 * Create a remainder from extra bytes at the end of the array.
	 * Remainder should always be null as we enter this routine.
	 * @param bytes
	 * @param nbytes count of bytes we need. 
	 * @return
	 */
	private byte[] truncateByteArray(byte[] bytes,int nbytes) {
		if( nbytes>bytes.length ) nbytes=bytes.length;
		if( nbytes==bytes.length ) return bytes;

		byte[] copy = new byte[nbytes];
		System.arraycopy(bytes, 0, copy, 0, nbytes);
		remainder = null;
		return copy;
	}
}

