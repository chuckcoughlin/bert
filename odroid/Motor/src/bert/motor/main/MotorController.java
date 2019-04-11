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
	 * Dynamixel documentation: No parity, 1 stop bit, 8 bits of data, no flow control
	 */
	public void initialize() {
		LOGGER.info(String.format("%s.initialize: Initializing port %s)",CLSS,port.getPortName()));
		try {
			boolean success = port.openPort();
			if( success && port.isOpened()) {
			port.setParams(BAUD_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);  
            //port.setEventsMask(SerialPort.MASK_RXCHAR);
            port.setEventsMask(0xFF);
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
	public void setStopped(boolean flag) { this.stopped = flag; }
	
	@Override
	public void receiveRequest(MessageBottle request) {
		lock.lock();
		try {
			
			if( isSingleGroupRequest(request)) {
				// Do nothing if the joint isn't in our group.
				String jointName = request.getProperty(BottleConstants.PROPERTY_JOINT, "");
				LOGGER.info(String.format("%s.receiveRequest: %s processing %s for %s",CLSS,group,request.fetchRequestType().name(),jointName));
				MotorConfiguration mc = configurations.get(jointName);
				if( mc==null ) { 
					return; 
				}
				else {
					LOGGER.info(String.format("%s.receiveRequest %s (%s): for %s",CLSS,group,request.fetchRequestType().name(),jointName));
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
				bytes = DxlMessage.bytesToBroadcastPing();   /// Stub
				writeBytesToSerial(bytes);
				LOGGER.info(String.format("%s.run: %s wrote %s",CLSS,group,DxlMessage.dump(bytes)));
				try {
					byte[] responseBytes = port.readBytes(4);
					LOGGER.info(String.format("%s.run: %s read %s",CLSS,group,DxlMessage.dump(responseBytes)));
				}
                catch (SerialPortException ex) {
                    System.out.println(ex);
                }	
			}
			catch(InterruptedException ie ) {}
			finally {
				lock.unlock();
			}
		}
	}
	
	
	// ============================= Private Helper Methods =============================
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
	
	private byte[] messageToBytes(MessageBottle request) {
		LOGGER.info(String.format("%s.messageToBytes: %s converting request",CLSS,group));
		byte[] bytes = null;
		if( request!=null) {
			RequestType type = request.fetchRequestType();
			if( type.equals(RequestType.GET_MOTOR_PROPERTY)) {
				String jointName = request.getProperty(BottleConstants.PROPERTY_JOINT, "");
				MotorConfiguration mc = configurations.get(jointName);
				JointProperty jp = JointProperty.valueOf(request.getProperty(BottleConstants.PROPERTY_PROPERTY, "NONE"));
				bytes = DxlMessage.bytesToGetProperty(mc.getId(),jp.name());
			}
			else if( type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
				bytes = DxlMessage.bytesToListProperty(request.getProperty(BottleConstants.PROPERTY_PROPERTY,"UNKNOWN"));
			}
			else {
				LOGGER.severe(String.format("%s.messageToBytes: Unhandled request type %s",CLSS,type.name()));
			}
			LOGGER.info(String.format("%s.messageToBytes: request (%s) is (%s)",CLSS,request.fetchRequestType(),DxlMessage.dump(bytes)));
		}
		return bytes;
	}
	
	// Already known to be a single group request. Here we are handling the response.
	private void updatePropertiesFromBytes(byte[] bytes,Map<String,String> props) {
		if( currentRequest!=null) {
			RequestType type = currentRequest.fetchRequestType();
			if( type.equals(RequestType.GET_MOTOR_PROPERTY)) {
				JointProperty jp = JointProperty.valueOf(currentRequest.getProperty(BottleConstants.PROPERTY_PROPERTY, "NONE"));
				if( jp.equals(JointProperty.POSITION)) {
					DxlMessage.updatePositionFromBytes(bytes,props);
				}
				else {
					LOGGER.severe(String.format("%s.bytesToProperties: Unhandled response to GET_MOTOR_PROPERTY for %s",CLSS,jp.name()));
				}
			}
			else {
				LOGGER.severe( String.format("%s.bytesToProperties: Unhandled response for %s",CLSS,type.name()));
			}
		}
	}
	
	private Map<Integer,Integer> bytesToPositions(byte[] bytes) {
		Map<Integer,Integer> positions = new HashMap<>();
		if( currentRequest!=null) {
			DxlMessage.updatePositionArrayFromBytes(bytes,positions);
		}
		return positions;
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
		LOGGER.info(String.format("%s.serialEvent callback!!! port %s: type %d",CLSS,event.getPortName(),event.getEventType()));
		if(event.isRXCHAR() && currentRequest!=null ){
        	// The value is the number of bytes in the read buffer
			int byteCount = event.getEventValue();
            if(byteCount>0){
                try {
                    byte[] bytes = port.readBytes(byteCount);
                    if( isSingleGroupRequest(currentRequest)) {
                    	updatePropertiesFromBytes(bytes,currentRequest.getProperties());
                    	motorManager.collectProperties(currentRequest.getProperties());
                    }
                    // We get a callback for every individual motor
                    else {
                    	Map<Integer,Integer> map = bytesToPositions(bytes);
                    	motorManager.collectPositions(map);
                    }
                }
                catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
        else if(event.isCTS()){
            if(event.getEventValue() == 1){
            	LOGGER.info(String.format("%s.serialEvent: CTS for %s is ON",CLSS,port.getPortName()));
            }
            else {
            	LOGGER.info(String.format("%s.serialEvent: CTS for %s is OFF",CLSS,port.getPortName()));
            }
        }
        else if(event.isDSR()){
            if(event.getEventValue() == 1){
            	LOGGER.info(String.format("%s.serialEvent: DSR for %s is ON",CLSS,port.getPortName()));
            }
            else {
            	LOGGER.info(String.format("%s.serialEvent: DSR for %s is OFF",CLSS,port.getPortName()));
            }
        }
    }
}
