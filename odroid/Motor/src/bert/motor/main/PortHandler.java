/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 *
 */
package bert.motor.main;

import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;

import bert.share.bottle.BottleConstants;
import bert.share.bottle.MessageBottle;
import bert.share.bottle.RequestType;
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
public class PortHandler implements Runnable, SerialPortEventListener {
	protected static final String CLSS = "MotorGroupHandler";
	private static System.Logger LOGGER = System.getLogger(CLSS);
	private static final int BAUD_RATE = 1000000;
	private final String group;                 // Group name
	private final SerialPort port;
	private boolean stopped = false;
	private final AggregatorInterface aggregator;
	private final Map<String,MotorConfiguration> configurations;

	public PortHandler(String name,SerialPort p,AggregatorInterface ag) {
		this.group = name;
		this.port = p;
		this.aggregator = ag;
		this.configurations = new HashMap<>();
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
			LOGGER.log(Level.ERROR, String.format("%s.close: Error closing port for %s (%s)",CLSS,group,spe.getLocalizedMessage()));
		}
	}
	public void open() {
		try {
			port.openPort();
			port.setParams(BAUD_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);  
            port.setEventsMask(SerialPort.MASK_RXCHAR);
            port.addEventListener(this);
		}
		catch(SerialPortException spe) {
			LOGGER.log(Level.ERROR, String.format("%s.open: Error opening port for %s (%s)",CLSS,group,spe.getLocalizedMessage()));
		}
	}
	public void setStopped(boolean flag) { this.stopped = flag; }
	public void setRequest(MessageBottle request) {
		if( isSingleGroupRequest(request)) {
			// Do nothing if the joint isn't in our group.
			String jointName = request.getProperty(BottleConstants.PROPERTY_JOINT, "");
			MotorConfiguration mc = configurations.get(jointName);
			if( mc!=null ) {
				
			}
		}
		// This is part of a request to all groups.
		else {
			
		}
	}
	
	public void run() {
		while( !stopped ) {
			 try{
				configurations.wait();
			}
			catch(InterruptedException ie ) {}
		}
	}
	
	// ============================= Privte Helper Methods =============================
	private boolean isSingleGroupRequest(MessageBottle msg) {
		if( msg.getRequestType().equals(RequestType.GET_CONFIGURATION) ||
			msg.getRequestType().equals(RequestType.SET_CONFIGURATION) ){
			return true;
		}
		return false;
	}
	// ============================== Serial Port Reader ===============================
	public void serialEvent(SerialPortEvent event) {
		if(event.isRXCHAR()){
        	// The value is the number of bytes in the read buffer.
            if(event.getEventValue() == 10){
                try {
                    byte buffer[] = port.readBytes(10);
                    configurations.notify();
                }
                catch (SerialPortException ex) {
                    System.out.println(ex);
                }
            }
        }
        else if(event.isCTS()){
            if(event.getEventValue() == 1){
            	LOGGER.log(Level.INFO, String.format("%s.serialEvent: CTS for group %s is ON",CLSS,group));
            }
            else {
            	LOGGER.log(Level.INFO, String.format("%s.serialEvent: CTS for group %s is OFF",CLSS,group));
            }
        }
        else if(event.isDSR()){
            if(event.getEventValue() == 1){
            	LOGGER.log(Level.INFO, String.format("%s.serialEvent: DSR for group %s is ON",CLSS,group));
            }
            else {
            	LOGGER.log(Level.INFO, String.format("%s.serialEvent: DSR for group %s is OFF",CLSS,group));
            }
        }
    }
}
