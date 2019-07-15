/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.motor.main;

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
import bert.motor.model.MessageWrapper;
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
 *  group are connected to the same serial port. We respond to the group controller
 *  using call-backs. The responses from the serial port do not necessarily keep
 *  to request boundaries. All we are sure of is that the requests are processed
 *  in order. 
 *  
 *  The configuration array has only those joints that are part of the group.
 *  It is important that the MotorConfiguration objects are the same objects
 *  (not clones) as those held by the MotorManager (MotorGroupController).
 */
public class MotorController implements  Runnable, SerialPortEventListener {
	protected static final String CLSS = "MotorController";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private static final int BAUD_RATE = 1000000;
	private static final int MIN_WRITE_INTERVAL = 50; // msecs between writes (25 was too short)
	private static final int STATUS_RESPONSE_LENGTH = 8; // byte count
	private final Condition running;
	private final Condition waitingOnSerial;
	private final String group;                 // Group name
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
		this.group = name;
		this.port = p;
		this.motorManager = mm;
		this.configurationsById = new HashMap<>();
		this.configurationsByName = new HashMap<>();
		this.requestQueue = new LinkedList<>();
		this.responseQueue = new LinkedList<>();
		this.lock = new ReentrantLock();
		this.running = lock.newCondition();
		this.waitingOnSerial = lock.newCondition();
		this.timeOfLastWrite = System.nanoTime()/1000000; 
	}

	public String getGroupName() { return this.group; }
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
	public void stop() {
		try {
			port.closePort();
		}
		catch(SerialPortException spe) {
			LOGGER.severe(String.format("%s.close: Error closing port for %s (%s)",CLSS,group,spe.getLocalizedMessage()));
		}
		stopped = true;
	}
	public void setStopped(boolean flag) { this.stopped = flag; }
	

	/**
	 * This method blocks until the prior request completes. 
	 * 
	 * @param request
	 */
	public void receiveRequest(MessageBottle request) {
		lock.lock();
		try {
			if( isSingleGroupRequest(request)) {
				// Do nothing if the joint isn't in our group.
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME,"UNKNOWN");
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
			requestQueue.addLast(request);
			running.signal();
		}
		finally {
			lock.unlock();
		}
	}
	
		
	
	/**
	 *  Wait until we receive a request message. Convert to serial request, write to port.
	 *  From there a listener forwards the responses to the group controller (MotorManager).
	 *  Do not free the request lock until we have the response in-hand.
	 */
	public void run() {
		while( !stopped ) {
			lock.lock();
			try{
				running.await();
				// LOGGER.info(String.format("%s.run: %s Got signal for message, writing to %s",CLSS,group,port.getPortName()));
				MessageBottle req = requestQueue.removeFirst();  // Oldest
				MessageWrapper wrapper = new MessageWrapper(req);
				if(isSingleWriteRequest(req) ) {
					byte[] bytes = messageToBytes(wrapper);
					if( bytes!=null ) {
						if( wrapper.getResponseCount()>0) {
							responseQueue.addLast(wrapper);
						}
						writeBytesToSerial(bytes);
						LOGGER.info(String.format("%s.run: %s wrote %d bytes",CLSS,group,bytes.length));
					}
				}
				else {
					List<byte[]> byteArrayList = messageToByteList(wrapper);
					if( wrapper.getResponseCount()>0) {
						responseQueue.addLast(wrapper);
					}
					for(byte[] bytes:byteArrayList) {
						writeBytesToSerial(bytes);
						LOGGER.info(String.format("%s.run: %s wrote %d bytes",CLSS,group,bytes.length));
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
	// Get the value of the supplied property from the specified array. Create a map to 
	// be aggregated by the MotorManager with similar responses from other motors.
	// The byte array may be the concatenation of several responses.
	private Map<Integer,String> createPropertyMapFromBytes(String propertyName,byte[] bytes) {
		Map<Integer,String> props = new HashMap<>();
		DxlMessage.updateParameterArrayFromBytes(propertyName,configurationsById,bytes,props);
		return props;
	}
	/**
	 * @param msg the request
	 * @return true if this is the type of request satisfied by a single controller.
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
	 * @return true if this is the type of message that translates into 
	 *         multiple writes to the serial port.
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
	 */
	private boolean returnsStatusArray(MessageBottle msg) {
		if( msg.fetchRequestType().equals(RequestType.LIST_MOTOR_PROPERTY))    {
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
			if( type.equals(RequestType.GET_GOALS)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				MotorConfiguration mc = configurationsByName.get(jointName);
				bytes = DxlMessage.bytesToGetGoals(mc.getId());
				wrapper.setResponseCount(1);   // Status message
			}
			else if( type.equals(RequestType.GET_LIMITS)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				MotorConfiguration mc = configurationsByName.get(jointName);
				bytes = DxlMessage.bytesToGetLimits(mc.getId());
				wrapper.setResponseCount(1);   // Status message
			}
			else if( type.equals(RequestType.GET_MOTOR_PROPERTY)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				MotorConfiguration mc = configurationsByName.get(jointName);
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, "");
				bytes = DxlMessage.bytesToGetProperty(mc.getId(),propertyName);
				wrapper.setResponseCount(1);   // Status message
			}
			else if( type.equals(RequestType.SET_MOTOR_PROPERTY)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "");
				MotorConfiguration mc = configurationsByName.get(jointName);
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, "");
				String value = request.getProperty(propertyName.toUpperCase(),"0.0");
				if( value!=null && !value.isEmpty()) {
					bytes = DxlMessage.bytesToSetProperty(mc,propertyName,Double.parseDouble(value));
					if(propertyName.equalsIgnoreCase("POSITION")) {
						long duration = mc.getTravelTime();
						if(request.getDuration()<duration) request.setDuration(duration);
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
			LOGGER.info(String.format("%s.messageToBytes: request(%s) = (%s)",CLSS,request.fetchRequestType(),DxlMessage.dump(bytes)));
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
				list = DxlMessage.byteArrayListToInitializePositions(configurationsByName.values());
				long duration = DxlMessage.getMostRecentTravelTime();
				if( request.getDuration()<duration ) request.setDuration(duration);
				wrapper.setResponseCount(0);  // No response
			}
			else if( type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, "");
				list = DxlMessage.byteArrayListToListProperty(propertyName,configurationsByName.values());
				wrapper.setResponseCount(list.size());  // Status packets from READ and BULK READ
			}
			else if( type.equals(RequestType.SET_POSE)) {
				String poseName = request.getProperty(BottleConstants.POSE_NAME, "");
				list = DxlMessage.byteArrayListToSetPose(configurationsByName,poseName);
				long duration = DxlMessage.getMostRecentTravelTime();
				if( request.getDuration()<duration ) request.setDuration(duration);
				wrapper.setResponseCount(0);  // AYNC WRITE, no responses
			}
			else {
				LOGGER.severe(String.format("%s.messageToByteList: Unhandled request type %s",CLSS,type.name()));
			}
			for(byte[] bytes:list) {
				LOGGER.info(String.format("%s.messageToByteList: request(%s) = (%s)",CLSS,request.fetchRequestType(),DxlMessage.dump(bytes)));
			}
		}
		return list;
	}
	
	/**
	 * We have just written a message to the serial port for which we will get no
	 * response. Make one up and send it off to the "MotorManager".  Currently there 
	 * are two request types in this category.
	 * @param msg the request
	 * @return true if this is the type of message that translates into 
	 *         multiple writes to the serial port.
	 */
	private void synthesizeResponse(MessageBottle msg) {

		if( msg.fetchRequestType().equals(RequestType.INITIALIZE_JOINTS) ||
			msg.fetchRequestType().equals(RequestType.SET_POSE)	) {
        	motorManager.handleSynthesizedResponse(msg);
        } 
		else  {
			LOGGER.severe( String.format("%s.synthesizeResponse: Unhandled response for %s",CLSS,msg.fetchRequestType().name()));
			motorManager.handleSingleMotorResponse(msg);  // Probably an error
        }
	}
	
	/**
	 * Operate on the supplied message directly. This is already known to be a single group request, 
	 * the first in the queue. Here we convert the request into a response.
	 * @param req
	 * @param bytes
	 */
	// We update the properties in the request from our serial message.
	// The properties must include motor type and orientation
	private void updateRequestFromBytes(MessageBottle request,byte[] bytes) {
		if( request!=null) {
			RequestType type = request.fetchRequestType();
			Map<String,String> properties = request.getProperties();
			if( type.equals(RequestType.GET_GOALS)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "UNKNOWN");
				MotorConfiguration mc = getMotorConfiguration(jointName);
				DxlMessage.updateGoalsFromBytes(mc,properties,bytes);
			} 
			else if( type.equals(RequestType.GET_LIMITS)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "UNKNOWN");
				MotorConfiguration mc = getMotorConfiguration(jointName);
				DxlMessage.updateLimitsFromBytes(mc,properties,bytes);
			} 
			else if( type.equals(RequestType.GET_MOTOR_PROPERTY)) {
				String jointName = request.getProperty(BottleConstants.JOINT_NAME, "UNKNOWN");
				MotorConfiguration mc = getMotorConfiguration(jointName);
				String propertyName = request.getProperty(BottleConstants.PROPERTY_NAME, "UNKNOWN");
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
					request.assignError(err);
				}
			} 
			else {
				LOGGER.severe( String.format("%s.updateCurrentRequestFromBytes: Unhandled response for %s",CLSS,type.name()));
			}
		}
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
				}
				timeOfLastWrite = now;
				boolean success = port.writeBytes(bytes);
				port.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);   // Force the write to complete
				if( !success ) {
					LOGGER.severe(String.format("%s.writeBytesToSerial: Failed write of %d bytes to %s",CLSS,bytes.length,port.getPortName()));
				}
			}
			catch(InterruptedException ignore) {}
			catch(SerialPortException spe) {
				LOGGER.severe(String.format("%s.writeBytesToSerial: Error writing to %s (%s)",CLSS,port.getPortName(),spe.getLocalizedMessage()));
			}
		}
	}
	// ============================== SerialPortEventListener ===============================
	/**
	 * Handle the response from the serial request. Note that all our interactions with removing from the
	 * responseQueue and dealing with remainder are synchronized. 
	 */
	public synchronized void serialEvent(SerialPortEvent event) {
		MessageWrapper wrapper = responseQueue.getFirst();
	
		if(event.isRXCHAR() && wrapper!=null ){
			MessageBottle req = wrapper.getMessage();
			// The value is the number of bytes in the read buffer
			int byteCount = event.getEventValue();
			LOGGER.info(String.format("%s.serialEvent callback port %s for %s: bytes %d",
										CLSS,event.getPortName(),req.fetchRequestType().name(),byteCount));
            if(byteCount>0){
                try {
                    byte[] bytes = port.readBytes(byteCount);
                    bytes = prependRemainder(bytes);
                    bytes = DxlMessage.ensureLegalStart(bytes);
                    LOGGER.info(String.format("%s.serialEvent: read = (%s)",CLSS,DxlMessage.dump(bytes)));
                    int nbytes = DxlMessage.getMessageLength(bytes);
                    if( nbytes<0 || bytes.length < nbytes) {
                    	LOGGER.info(String.format("%s.serialEvent Message too short (%d), requires additional read",CLSS,nbytes));
                    	return;
                    }
                    if( returnsStatusArray(req) ) {
                    	int nmsgs = nbytes/STATUS_RESPONSE_LENGTH;
                    	if( nmsgs>wrapper.getResponseCount() ) nmsgs = wrapper.getResponseCount();
                    	nbytes = nmsgs*STATUS_RESPONSE_LENGTH;
                    }
                    if( nbytes<bytes.length ) {
                    	bytes = truncateByteArray(bytes,nbytes);
                    }
                    if( isSingleGroupRequest(req)) {
                    	updateRequestFromBytes(req,bytes);
                    	responseQueue.removeFirst();
                    	motorManager.handleSingleMotorResponse(req);
                    }
                    // Ultimately we get a callback for every individual motor. Any errors get swallowed, but logged.
                    else {
                    	String propertyName = req.getProperty(BottleConstants.PROPERTY_NAME, "NONE");
                    	Map<Integer,String> map = createPropertyMapFromBytes(propertyName,bytes);
                    	for( Integer key:map.keySet() ) {
            				String param = map.get(key);
            				String name = configurationsById.get(key).getName().name();
            				req.setJointValue(name, param);
            				wrapper.decrementResponseCount();
            				LOGGER.info(String.format("%s.aggregateMotorProperties: received %s (%d remaining) = %s",
            						CLSS,name,wrapper.getResponseCount(),param));
            			}
                    	if( wrapper.getResponseCount()<=0 ) {
                    		responseQueue.removeFirst();
                    		motorManager.handleAggregatedResponse(req);
                    	}
                    }
                }
                catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
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

