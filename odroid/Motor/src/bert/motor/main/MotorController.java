/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.motor.main;

import java.util.HashMap;
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
import bert.share.motor.JointProperty;
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
	private final Map<String,MotorConfiguration> configurations;
	private MessageBottle currentRequest;

	public MotorController(String name,SerialPort p,MotorManager mm) {
		this.group = name;
		this.port = p;
		this.motorManager = mm;
		this.configurations = new HashMap<>();
		this.currentRequest = null;
		this.lock = new ReentrantLock();
		this.running = lock.newCondition();
	}

	public String getGroupName() { return this.group; }
	public Map<String,MotorConfiguration> getConfigurations() { return this.configurations; }
	public MotorConfiguration getMotorConfiguration(String name) { return configurations.get(name); }
	public void putMotorConfiguration(String name,MotorConfiguration mc) {
		configurations.put(name, mc);
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
	public int getMotorCount() { return configurations.size(); }
	/**
	 * Configure the physical motors in the group, synchronizing their settings with the
	 * contents of the configuration file. This will trigger the SerialEvent call-backs,
	 * but the request is null, so these get ignored.
	 */
	@Override
	public void start() {
		/*
		for(String key:configurations.keySet()) {
			MotorConfiguration mc = configurations.get(key);
			int id = mc.getId();
			byte[] bytes = PortTest.bytesToSetSpeed(id,mc.getType(),mc.getSpeed());
			writeBytesToSerial(bytes);
			bytes = PortTest.bytesToSetTorque(id,mc.getType(),mc.getTorque());
			writeBytesToSerial(bytes);
		}
		*/
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
				MotorConfiguration mc = configurations.get(jointName);
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
		
	
	public void run() {
		while( !stopped ) {
			lock.lock();
			try{
				running.await();
				LOGGER.info(String.format("%s.run: %s Got signal for message, writing to %s",CLSS,group,port.getPortName()));
				byte[] bytes = messageToBytes(currentRequest);
				writeBytesToSerial(bytes);
				LOGGER.info(String.format("%s.run: %s wrote %d bytes",CLSS,group,bytes.length));	
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
	private Map<Integer,String> createPropertyMapFromBytes(String propertyName,byte[] bytes) {
		Map<Integer,String> props = new HashMap<>();
		if( currentRequest!=null) {
			DxlMessage.updateParameterArrayFromBytes(propertyName,props,bytes);
		}
		return props;
	}
	
	/**
	 * @param msg the request
	 * @return true if this is the type of message satisfied by a single controller.
	 */
	private boolean isSingleGroupRequest(MessageBottle msg) {
		if( msg.fetchRequestType().equals(RequestType.GET_MOTOR_PROPERTY) ||
			msg.fetchRequestType().equals(RequestType.SET_MOTOR_PROPERTY) ){
			return true;
		}
		return false;
	}
	
	// Convert the request message into a command for the serial port
	private byte[] messageToBytes(MessageBottle request) {
		byte[] bytes = null;
		if( request!=null) {
			RequestType type = request.fetchRequestType();
			if( type.equals(RequestType.GET_MOTOR_PROPERTY)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				MotorConfiguration mc = configurations.get(jointName);
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, "");
				bytes = DxlMessage.bytesToGetProperty(mc.getId(),propertyName);
			}
			else if( type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, "");
				bytes = DxlMessage.bytesToListProperty(propertyName);
			}
			else {
				LOGGER.severe(String.format("%s.messageToBytes: Unhandled request type %s",CLSS,type.name()));
			}
			LOGGER.info(String.format("%s.messageToBytes: request(%s) = (%s)",CLSS,request.fetchRequestType(),DxlMessage.dump(bytes)));
		}
		return bytes;
	}
	
	// Already known to be a single group request. Here we are readying the response.
	// We update the properties in the request from our serial message.
	private void updateCurrentRequestFromBytes(byte[] bytes) {
		if( currentRequest!=null) {
			RequestType type = currentRequest.fetchRequestType();
			Map<String,String> properties = currentRequest.getProperties();
			if( type.equals(RequestType.GET_MOTOR_PROPERTY)) {
				String jointName = currentRequest.getProperty(BottleConstants.JOINT_NAME, "");
				DxlMessage.updateParameterFromBytes(jointName,properties,bytes);
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
				if( !success ) {
					LOGGER.severe(String.format("%s.writeBytesToSerial: Failed write of %d bytes to %s",CLSS,bytes.length));
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
