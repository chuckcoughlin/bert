/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.command.controller;

import java.util.logging.Logger;


/**
 *  The socket requires the MAC address of the target paired device and the UUID
 *  of the service that it provides. It encapsulates a bi-directional
 *  connection between client and server used for text messages.
 *  
 *  This class is currently gutted waiting for BlueCove implementation.
 */
public class BluetoothSocket   {
	private static final String CLSS = "BluetoothSocket";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private static final long CLIENT_ATTEMPT_INTERVAL = 2000;  // 2 secs
	private static final int CLIENT_LOG_INTERVAL = 10;
	private static final int READ_POLL_INTERVAL = 60000; 
	private final String mac;
	private final String uuid;

	/**
	 * Constructor:
	 * @param mac identifier of the target paired device
	 * @param uuid of the service provided by the client
	 */
	public BluetoothSocket(String mac,String uuid) {
		this.mac = mac;
		this.uuid = uuid;
	}
	
	public String getName() {return CLSS;}
	
	
	/**
	 * If we are a server, create a listener and wait to accept a connection.
	 * There is no action for a client.
	 */
	public boolean discover() {
		LOGGER.warning(String.format("%s.discover: Getting bluetooth manager ...", CLSS));
		boolean success = true;
	
		
		return success;
	}

	/**
	 * This must not be called before the socket is created.
	 * Open IO streams for reading and writing.
	 */
	public void startup() {
		LOGGER.warning(String.format("%s.startup: before discovery completed",CLSS));

	}
	
	/**
	 * Release native memory.
	 */
	public void shutdown() {
	}

	/**
	 * Read a line of text from the device characteristic. We poll, but the read will block and wait for data to appear. 
	 *
	 * @return text from the device.
	 */
	public String read() {
		String text = null;

		return text;
	}
	
	
	/**
	 * Write plain text to the socket.
	 */
	public void write(String text) {


	}
}

