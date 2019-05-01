/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.motor.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import bert.motor.dynamixel.DxlMessage;
import bert.share.controller.Controller;
import bert.share.message.BottleConstants;
import bert.share.message.MessageBottle;
import bert.share.message.RequestType;
import bert.share.motor.Joint;
import bert.share.motor.MotorConfiguration;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 *  Handle requests directed to a specific group of motors. All motors in the 
 *  group are connected to the same serial port. We respond using call-backs.
 *  
 *  The configuration array has only those joints that are part of the group.
 */
public class MotorController implements Controller, Runnable, SerialPortEventListener {
	protected static final String CLSS = "MotorController";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static final int BAUD_RATE = 1000000;
	private final Condition running;
	private final String group;                 // Group name
	private final Lock lock;
	private final SerialPort port;
	private boolean stopped = false;
	private final MotorManager motorManager;
	private final Map<Integer,MotorConfiguration> configurationsById;
	private final Map<String,MotorConfiguration> configurationsByName;
	private MessageBottle currentRequest;

	public MotorController(String name,SerialPort p,MotorManager mm) {
		this.group = name;
		this.port = p;
		this.motorManager = mm;
		this.configurationsById = new HashMap<>();
		this.configurationsByName = new HashMap<>();
		this.currentRequest = null;
		this.lock = new ReentrantLock();
		this.running = lock.newCondition();
	}

	public String getGroupName() { return this.group; }
	public Map<String,MotorConfiguration> getConfigurations() { return this.configurationsByName; }
	public MotorConfiguration getMotorConfiguration(String name) { return configurationsByName.get(name); }
	public void putMotorConfiguration(String name,MotorConfiguration mc) {
		configurationsById.put(mc.getId(), mc);
		configurationsByName.put(name, mc);
	}
	
	@Override
	public void stop() {
		try {
			port.closePort();
		}
		catch(SerialPortException spe) {
			LOGGER.severe(String.format("%s.close: Error closing port for %s (%s)",CLSS,group,spe.getLocalizedMessage()));
		}
		stopped = true;
	}
	
	/**
	 * @return the number of motors controlled by this controller
	 */
	public int getMotorCount() { return configurationsByName.size(); }
	/**
	 * Configure the physical motors in the group, synchronizing their settings with the
	 * contents of the configuration file. This will trigger the SerialEvent call-backs,
	 * but the request is null, so these get ignored.
	 */
	@Override
	public void start() {

		List<byte[]>messages = DxlMessage.byteArrayListToInitializeRAM(configurationsByName);
		for(byte[] bytes:messages) {
			writeBytesToSerial(bytes);
			LOGGER.info(String.format("%s.start: %s wrote %d bytes to initialize",CLSS,group,bytes.length));
		}
	}
	
	/**
	 * Open and configure the port.
	 * Dynamixel documentation: No parity, 1 stop bit, 8 bits of data, no flow control
	 */
	public void initialize() {
		LOGGER.info(String.format("%s.initialize: Initializing port %s)",CLSS,port.getPortName()));
		if( !port.isOpened()) {
			try {
				boolean success = port.openPort();
				if( success && port.isOpened()) {
					port.setParams(BAUD_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);  
					port.setEventsMask(SerialPort.MASK_RXCHAR);
					port.purgePort(SerialPort.PURGE_RXCLEAR);
					port.purgePort(SerialPort.PURGE_TXCLEAR);
					port.addEventListener(this);
				}
				else {
					LOGGER.severe(String.format("%s.initialize: Failed to open port %s for %s",CLSS,port.getPortName(),group));
				}
			}
			catch(SerialPortException spe) {
				LOGGER.severe(String.format("%s.initialize: Error opening port %s for %s (%s)",CLSS,port.getPortName(),group,spe.getLocalizedMessage()));
			}
			LOGGER.info(String.format("%s.initialize: Initialized port %s)",CLSS,port.getPortName()));
		}
	}
	public void setStopped(boolean flag) { this.stopped = flag; }
	
	@Override
	public void receiveRequest(MessageBottle request) {
		lock.lock();
		try {
			if( isSingleGroupRequest(request)) {
				// Do nothing if the joint isn't in our group.
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				String propertyName = request.getProperty(jointName,"UNKNOWN");
				MotorConfiguration mc = configurationsByName.get(jointName);
				if( mc==null ) { 
					return; 
				}
				else {
					LOGGER.info(String.format("%s.receiveRequest %s (%s): for %s (%s)",CLSS,group,request.fetchRequestType().name(),
							jointName,propertyName));
				}
			}
			else {
				LOGGER.info(String.format("%s.receiveRequest: %s non-single group request (%s)",CLSS,group,request.fetchRequestType().name()));
			}
			currentRequest = request;
			running.signal();
		}
		finally {
			lock.unlock();
		}
	}
	
	@Override
	public void receiveResponse(MessageBottle response) {}
		
	
	// Wait until we receive a request message. Convert to serial request, write to port.
	// From there a listener forwards the responses to the group controller (MotorManager)
	public void run() {
		while( !stopped ) {
			lock.lock();
			try{
				running.await();
				LOGGER.info(String.format("%s.run: %s Got signal for message, writing to %s",CLSS,group,port.getPortName()));
				if(isSingleWriteRequest(currentRequest) ) {
					byte[] bytes = messageToBytes(currentRequest);
					writeBytesToSerial(bytes);
					LOGGER.info(String.format("%s.run: %s wrote %d bytes",CLSS,group,bytes.length));
				}
				else {
					List<byte[]> byteArrayList = messageToByteList(currentRequest);
					for(byte[] bytes:byteArrayList) {
						writeBytesToSerial(bytes);
						LOGGER.info(String.format("%s.run: %s wrote %d bytes",CLSS,group,bytes.length));
					}
				}	
			}
			catch(InterruptedException ie ) {}
			finally {
				lock.unlock();
			}
		}
	}
	
	
	// ============================= Private Helper Methods =============================
	// Get the value of the supplied property from the specified array. Create a map to 
	// be aggregated by the MotorManager with similar responses from other motors.
	// The byte array may be the concatenation of several responses.
	private Map<Integer,String> createPropertyMapFromBytes(String propertyName,byte[] bytes) {
		Map<Integer,String> props = new HashMap<>();
		if( currentRequest!=null) {
			DxlMessage.updateParameterArrayFromBytes(propertyName,configurationsById,bytes,props);
		}
		return props;
	}
	
	/**
	 * @param msg the request
	 * @return true if this is the type of message satisfied by a single controller.
	 */
	private boolean isSingleGroupRequest(MessageBottle msg) {
		if( msg.fetchRequestType().equals(RequestType.GET_GOALS) 		 ||
			msg.fetchRequestType().equals(RequestType.GET_LIMITS)  		 ||
			msg.fetchRequestType().equals(RequestType.GET_MOTOR_PROPERTY)||
			msg.fetchRequestType().equals(RequestType.SET_MOTOR_PROPERTY)  ){
			return true;
		}
		return false;
	}
	
	/**
	 * @param msg the request
	 * @return true if this is the type of message that translates into a 
	 *         single write to the serial port.
	 */
	private boolean isSingleWriteRequest(MessageBottle msg) {
		if( msg.fetchRequestType().equals(RequestType.LIST_MOTOR_PROPERTY)){
			return false;
		}
		return true;
	}
	
	// Convert the request message into a command for the serial port
	private byte[] messageToBytes(MessageBottle request) {
		byte[] bytes = null;
		if( request!=null) {
			RequestType type = request.fetchRequestType();
			if( type.equals(RequestType.GET_GOALS)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				MotorConfiguration mc = configurationsByName.get(jointName);
				bytes = DxlMessage.bytesToGetGoals(mc.getId());
			}
			else if( type.equals(RequestType.GET_LIMITS)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				MotorConfiguration mc = configurationsByName.get(jointName);
				bytes = DxlMessage.bytesToGetLimits(mc.getId());
			}
			else if( type.equals(RequestType.GET_MOTOR_PROPERTY)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				MotorConfiguration mc = configurationsByName.get(jointName);
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, "");
				bytes = DxlMessage.bytesToGetProperty(mc.getId(),propertyName);
			}
			else if( type.equals(RequestType.SET_MOTOR_PROPERTY)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				MotorConfiguration mc = configurationsByName.get(jointName);
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, "");
				String value = request.getProperty(propertyName.toUpperCase(),"0.0");
				bytes = DxlMessage.bytesToSetProperty(mc,propertyName,Double.parseDouble(value));
			}
			else {
				LOGGER.severe(String.format("%s.messageToBytes: Unhandled request type %s",CLSS,type.name()));
			}
			LOGGER.info(String.format("%s.messageToBytes: request(%s) = (%s)",CLSS,request.fetchRequestType(),DxlMessage.dump(bytes)));
		}
		return bytes;
	}
	
	// Convert the request message into a list of commands for the serial port
	private List<byte[]> messageToByteList(MessageBottle request) {
		List<byte[]> list = new ArrayList<>();
		if( request!=null) {
			RequestType type = request.fetchRequestType();
			// Unfortunately a broadcast request does not work here. We have to concatenate the
			// requests into a single long list.
			if( type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, "");
				list = DxlMessage.byteArrayListToListProperty(propertyName,configurationsByName.values());
			}
			else {
				LOGGER.severe(String.format("%s.messageToBytes: Unhandled request type %s",CLSS,type.name()));
			}
			for(byte[] bytes:list) {
				LOGGER.info(String.format("%s.messageToBytes: request(%s) = (%s)",CLSS,request.fetchRequestType(),DxlMessage.dump(bytes)));
			}
		}
		return list;
	}
	
	// Already known to be a single group request. Here we are readying the response.
	// We update the properties in the request from our serial message.
	// The properties must include motor type and orientation
	private void updateCurrentRequestFromBytes(byte[] bytes) {
		if( currentRequest!=null) {
			RequestType type = currentRequest.fetchRequestType();
			Map<String,String> properties = currentRequest.getProperties();
			if( type.equals(RequestType.GET_GOALS)) {
				String jointName = currentRequest.getProperty(BottleConstants.JOINT_NAME, "UNKNOWN");
				MotorConfiguration mc = getMotorConfiguration(jointName);
				DxlMessage.updateGoalsFromBytes(mc,properties,bytes);
			} 
			else if( type.equals(RequestType.GET_LIMITS)) {
				String jointName = currentRequest.getProperty(BottleConstants.JOINT_NAME, "UNKNOWN");
				MotorConfiguration mc = getMotorConfiguration(jointName);
				DxlMessage.updateLimitsFromBytes(mc,properties,bytes);
			} 
			else if( type.equals(RequestType.GET_MOTOR_PROPERTY)) {
				String jointName = currentRequest.getProperty(BottleConstants.JOINT_NAME, "UNKNOWN");
				MotorConfiguration mc = getMotorConfiguration(jointName);
				String propertyName = currentRequest.getProperty(BottleConstants.PROPERTY_NAME, "UNKNOWN");
				DxlMessage.updateParameterFromBytes(propertyName,mc,properties,bytes);
				String partial = properties.get(BottleConstants.TEXT);
				if( partial!=null && !partial.isEmpty()) {
					Joint joint = Joint.valueOf(jointName);
					properties.put(BottleConstants.TEXT,String.format("My %s %s is %s", Joint.toText(joint),propertyName.toLowerCase(),partial));
				}	
			} 
			// The only interesting response is the error code.
			else if( type.equals(RequestType.SET_MOTOR_PROPERTY)) {
				String err =  DxlMessage.errorMessageFromStatus(bytes);
				if( err!=null && !err.isEmpty() ) {
					currentRequest.assignError(err);
				}
			} 
			else {
				LOGGER.severe( String.format("%s.updateCurrentRequestFromBytes: Unhandled response for %s",CLSS,type.name()));
			}
		}
	}
	

	
	private void writeBytesToSerial(byte[] bytes) {
		if( bytes!=null && bytes.length>0 ) {
			try {
				boolean success = port.writeBytes(bytes);
				port.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);   // Force the write to complete
				if( !success ) {
					LOGGER.severe(String.format("%s.writeBytesToSerial: Failed write of %d bytes to %s",CLSS,bytes.length,port.getPortName()));
				}
			}
			catch(SerialPortException spe) {
				LOGGER.severe(String.format("%s.writeBytesToSerial: Error writing to %s (%s)",CLSS,port.getPortName(),spe.getLocalizedMessage()));
			}
		}
	}
	// ============================== SerialPortEventListener ===============================
	/**
	 * Handle the response from the serial request. 
	 */
	public void serialEvent(SerialPortEvent event) {
		if(event.isRXCHAR() && currentRequest!=null ){
			// The value is the number of bytes in the read buffer
			int byteCount = event.getEventValue();
			LOGGER.info(String.format("%s.serialEvent callback port %s: bytes %d",CLSS,event.getPortName(),byteCount));
            if(byteCount>0){
                try {
                    byte[] bytes = port.readBytes(byteCount);
                    LOGGER.info(String.format("%s.serialEvent: read = (%s)",CLSS,DxlMessage.dump(bytes)));
                    if( isSingleGroupRequest(currentRequest)) {
                    	updateCurrentRequestFromBytes(bytes);
                    	motorManager.handleUpdatedProperties(currentRequest.getProperties());
                    }
                    // We get a callback for every individual motor. Any errors get swallowed, but logged.
                    else {
                    	String propertyName = currentRequest.getProperty(BottleConstants.PROPERTY_NAME, "NONE");
                    	Map<Integer,String> map = createPropertyMapFromBytes(propertyName,bytes);
                    	motorManager.aggregateMotorProperties(map);
                    }
                }
                catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
    }
}
