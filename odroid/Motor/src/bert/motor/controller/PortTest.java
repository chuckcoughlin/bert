/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package bert.motor.controller;

import bert.motor.dynamixel.DxlMessage;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 * Exercise a port connected to the Dynamixels.
 */
@SuppressWarnings("unused")
public class PortTest implements Runnable,SerialPortEventListener {
	private static final int BAUD_RATE = 1000000;
	private static final String DEVICE = "/dev/ttyACM0";
	private final SerialPort port;
	private final DxlMessage dxl;
	
	public PortTest() {
		port = new SerialPort(DEVICE);
		dxl = new DxlMessage();
	}
	
	public void run() {
		boolean success = true;
		try {
			success = port.closePort();
			System.out.println(String.format("PortTest.close: Success = %s",(success?"true":"false")));
			
		}
		catch(SerialPortException ignore) {}
		
			
		try {	
			// Open the port
			delay();
			success = port.openPort();
			System.out.println(String.format("PortTest.open: Success = %s",(success?"true":"false")));
			System.out.println(String.format("PortTest.open: isOpened = %s",(port.isOpened()?"true":"false")));

			// Configure the port
			delay();
			success = port.setParams(BAUD_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE,false,false);
			System.out.println(String.format("PortTest.setParams: Success = %s",(success?"true":"false")));

			delay();
			success = port.setEventsMask(SerialPort.MASK_RXCHAR);
			System.out.println(String.format("PortTest.setEventsMask: Success = %s",(success?"true":"false")));

			delay();
			success = port.purgePort(SerialPort.PURGE_RXCLEAR);
			success = success&&port.purgePort(SerialPort.PURGE_TXCLEAR);
			System.out.println(String.format("PortTest.purgePort: Success = %s",(success?"true":"false")));

			delay();
			port.addEventListener(this);
		}
		catch(SerialPortException spe) {
			System.out.println(String.format("PortTest: Error opening/configuring port %s (%s)",DEVICE,spe.getLocalizedMessage()));
		}
		// Write
		delay();
		byte[] bytes = dxl.bytesToBroadcastPing();
		try {
			// Write the buffer
			success = port.writeBytes(bytes);
			System.out.println(String.format("PortTest.writeBytes: Success = %s writing %d bytes ",(success?"true":"false"),bytes.length));
		}
		catch(SerialPortException spe) {
			System.out.println(String.format("PortTest: Error writing %s (%s)",dxl.dump(bytes),spe.getLocalizedMessage()));
		}

		// Read
		delay();
		try {
			// Read the port
			int incount  = port.getInputBufferBytesCount();
			int outcount = port.getOutputBufferBytesCount();
			System.out.println(String.format("PortTest.getInputBufferBytesCount: %d bytes ready to read, %d bytes to write",incount,outcount));
			if( incount>0 ) {
			bytes = port.readBytes();
			if( bytes!=null )
			System.out.println(String.format("PortTest.readBytes: Got %d bytes",bytes.length));
			System.out.println(String.format("PortTest.readBytes: %s",dxl.dump(bytes)));
			}
		}
		catch(SerialPortException spe) {
			System.out.println(String.format("PortTest: Error reading (%s)",spe.getLocalizedMessage()));
		}
		
		// Close
		delay();
		try {
			success = port.closePort();
			System.out.println(String.format("PortTest.close: Success = %s",(success?"true":"false")));
		}
		catch(SerialPortException spe) {
			System.out.println(String.format("PortTest.close: Error closing port (%s)",spe.getLocalizedMessage()));;
		}
	}
	/**
	 * Provide some time spacing between test steps
	 */
	private void delay() {
		try {
			Thread.sleep(1000);
		}
		catch(InterruptedException ignore) {}
	}
	
	// ============================== SerialPortEventListener ===============================
	/**
	 * Handle the response from the serial request. 
	 */
	public void serialEvent(SerialPortEvent event) {
		System.out.println("PotTest: Got a serial event ");
	}
	/**
	 * Open a port. Write and read.
	 */
	public static void main(String [] args) {
		Thread tester = new Thread(new PortTest());
		tester.start();
    }
}