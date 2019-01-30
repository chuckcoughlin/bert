/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.motor.main;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import bert.motor.dynamixel.DxlMessage;
import bert.share.bottle.BottleConstants;
import bert.share.bottle.MessageBottle;
import bert.share.bottle.RequestType;
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
public class MotorController implements Runnable, SerialPortEventListener {
	protected static final String CLSS = "MotorController";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static final int BAUD_RATE = 1000000;
	private final Condition running;
	private final String group;                 // Group name
	private final Lock lock;
	private final SerialPort port;
	private boolean stopped = false;
	private final MotorManagerInterface motorManager;
	private final Map<String,MotorConfiguration> configurations;
	private MessageBottle request;

	public MotorController(String name,SerialPort p,MotorManagerInterface mm) {
		this.group = name;
		this.port = p;
		this.motorManager = mm;
		this.configurations = new HashMap<>();
		this.request = null;
		this.lock = new ReentrantLock();
		this.running = lock.newCondition();
	}

	public String getGroupName() { return this.group; }
	public MotorConfiguration getMotorConfiguration(String name) { return configurations.get(name); }
	public void putMotorConfiguration(String name,MotorConfiguration mc) {
		configurations.put(name, mc);
	}
	
	public void close() {
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
	public void initialize() {
		for(String key:configurations.keySet()) {
			MotorConfiguration mc = configurations.get(key);
			int id = mc.getId();
			byte[] bytes = DxlMessage.bytesToSetSpeed(id,mc.getType(),mc.getSpeed());
			writeBytesToSerial(bytes);
			bytes = DxlMessage.bytesToSetTorque(id,mc.getType(),mc.getTorque());
			writeBytesToSerial(bytes);
		}
	}
	
	public void open() {
		try {
			port.openPort();
			port.setParams(BAUD_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);  
            port.setEventsMask(SerialPort.MASK_RXCHAR);
            port.purgePort(SerialPort.PURGE_RXCLEAR);
            port.purgePort(SerialPort.PURGE_TXCLEAR);
            port.addEventListener(this);
		}
		catch(SerialPortException spe) {
			LOGGER.severe(String.format("%s.open: Error opening port for %s (%s)",CLSS,group,spe.getLocalizedMessage()));
		}
	}
	public void setStopped(boolean flag) { this.stopped = flag; }
	
	public void processRequest(MessageBottle request) {
		if( isSingleGroupRequest(request)) {
			// Do nothing if the joint isn't in our group.
			String jointName = request.getProperty(BottleConstants.PROPERTY_JOINT, "");
			MotorConfiguration mc = configurations.get(jointName);
			if( mc==null ) {
				return;  
			}
		}
		running.signal();
	}
	
	public void run() {
		while( !stopped ) {
			lock.lock();
			try{
				running.await();
				byte[] bytes = messageToBytes(request);
				writeBytesToSerial(bytes);
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
		if( msg.getRequestType().equals(RequestType.GET_CONFIGURATION) ||
			msg.getRequestType().equals(RequestType.SET_CONFIGURATION) ){
			return true;
		}
		return false;
	}
	
	private byte[] messageToBytes(MessageBottle request) {
		byte[] bytes = null;
		if( request!=null) {
			RequestType type = request.getRequestType();
			if( type.equals(RequestType.GET_CONFIGURATION)) {
				String jointName = request.getProperty(BottleConstants.PROPERTY_JOINT, "");
				MotorConfiguration mc = configurations.get(jointName);
				JointProperty jp = JointProperty.valueOf(request.getProperty(BottleConstants.PROPERTY_PROPERTY, "NONE"));
				if( jp.equals(JointProperty.POSITION)) {
					bytes = DxlMessage.bytesToGetPosition(mc.getId());
				}
				else {
					LOGGER.severe(String.format("%s.messageToBytes: Unimplemented GET_CONFIGURATION for %s",CLSS,jp.name()));
				}
			}
			else {
				LOGGER.severe(String.format("%s.messageToBytes: Unhandled request type %s",CLSS,type.name()));
			}
		}
		
		return bytes;
	}
	
	// Already known to be a single group request. Here we are handling the response.
	private void updatePropertiesFromBytes(byte[] bytes,Properties props) {
		if( request!=null) {
			RequestType type = request.getRequestType();
			if( type.equals(RequestType.GET_CONFIGURATION)) {
				JointProperty jp = JointProperty.valueOf(request.getProperty(BottleConstants.PROPERTY_PROPERTY, "NONE"));
				if( jp.equals(JointProperty.POSITION)) {
					DxlMessage.updatePositionFromBytes(bytes,props);
				}
				else {
					LOGGER.severe(String.format("%s.bytesToProperties: Unhandled response to GET_CONFIGURATION for %s",CLSS,jp.name()));
				}
			}
			else {
				LOGGER.severe( String.format("%s.bytesToProperties: Unhandled response for %s",CLSS,type.name()));
			}
		}
	}
	
	private Map<Integer,Integer> bytesToPositions(byte[] bytes) {
		Map<Integer,Integer> positions = new HashMap<>();
		if( request!=null) {
			DxlMessage.updatePositionArrayFromBytes(bytes,positions);
		}
		return positions;
	}
	
	private void writeBytesToSerial(byte[] bytes) {
		if( bytes!=null ) {
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
	// ============================== Serial Port Reader ===============================
	/**
	 * Handle the response from the serial request. 
	 */
	public void serialEvent(SerialPortEvent event) {
		if(event.isRXCHAR() && request!=null ){
        	// The value is the number of bytes in the read buffer
			int byteCount = event.getEventValue();
            if(byteCount>0){
                try {
                    byte[] bytes = port.readBytes(byteCount);
                    if( isSingleGroupRequest(request)) {
                    	updatePropertiesFromBytes(bytes,request.getProperties());
                    	motorManager.collectProperties(request.getProperties());
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
