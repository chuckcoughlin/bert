/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import java.util.List;
import java.util.logging.Logger;

import tinyb.BluetoothDevice;
import tinyb.BluetoothException;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;
import tinyb.BluetoothManager;
import tinyb.BluetoothNotification;
/**
 *  The socket requires the MAC address of the target paired device and the UUID
 *  of the service that it provides. It encapsulates a bi-directional
 *  connection between client and server used for text messages.
 *  
 *  The file descriptors are opened on "startup" and closed on 
 *  "shutdown". Change listeners are notified (in a separate Thread) when the
 *  socket is "ready".
 */
public class BluetoothSocket   {
	private static final String CLSS = "BluetoothSocket";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private static final long CLIENT_ATTEMPT_INTERVAL = 2000;  // 2 secs
	private static final int CLIENT_LOG_INTERVAL = 10;
	private static final int READ_POLL_INTERVAL = 60000; 
	private final String mac;
	private final String uuid;
	private BluetoothGattCharacteristic characteristic;
	private BluetoothDevice device;
	private BluetoothGattService service;

	/**
	 * Constructor:
	 * @param mac identifier of the target paired device
	 * @param uuid of the service provided by the client
	 */
	public BluetoothSocket(String mac,String uuid) {
		this.mac = mac;
		this.uuid = uuid;  
		this.device = null;
		this.service = null;
		this.characteristic = null;
	}
	
	public String getName() {return CLSS;}
	
	
	/**
	 * If we are a server, create a listener and wait to accept a connection.
	 * There is no action for a client.
	 */
	public boolean discover() {
		BluetoothManager manager = BluetoothManager.getBluetoothManager();
		boolean success = manager.startDiscovery();
		if( !success ) {
			LOGGER.warning(String.format("%s.discover: Failed to start discovery", CLSS));
		}
		else {
			int attempts = 1;
			success = false;
			while(!success ) {
				// Keep polling for the desired device 
				try  {
					List<BluetoothDevice>devices = manager.getDevices();
					for(BluetoothDevice dev:devices) {
						LOGGER.info(String.format("%s.discover: found paired device %s at %s", CLSS,dev.getName(),dev.getAddress()));
						if( dev.getAddress().equalsIgnoreCase(mac)) {
							List<BluetoothGattService> services = dev.getServices();
							for(BluetoothGattService serv:services) {
								if( serv.getUUID().equalsIgnoreCase(uuid)) {
									this.service = serv;
									this.device = dev;
									this.characteristic = serv.getCharacteristics().get(0); // Assume only one characteristic
									success = true;
									break;
								}
							}
						}
					}
					Thread.sleep(CLIENT_ATTEMPT_INTERVAL);
				}

				catch(InterruptedException ie) {
					if( attempts%CLIENT_LOG_INTERVAL==0) {
						LOGGER.warning(String.format("%s.discover: Strii trying ..", CLSS));
					}
				}
			}
			attempts++;
		}
		
		return success;
	}

	/**
	 * This must not be called before the socket is created.
	 * Open IO streams for reading and writing.
	 */
	public void startup() {
		if( characteristic==null ) {
			LOGGER.warning(String.format("%s.startup: before discovery completed",CLSS));
		}
		else {
			characteristic.enableValueNotifications(new StringNotification());
		}
	}
	
	/**
	 * Release native memory.
	 */
	public void shutdown() {
		characteristic.disableValueNotifications();
		characteristic.close();
		service.close();
		device.close();
	}

	/**
	 * Read a line of text from the device characteristic. We poll, but the read will block and wait for data to appear. 
	 *
	 * @return text from the device.
	 */
	public String read() {
		String text = null;
		if(characteristic!=null )  {
			try {
				for(;;) {
					byte[] raw = characteristic.readValue();
					text = new String(raw);
					if( !text.isEmpty()) break;
					Thread.sleep(READ_POLL_INTERVAL);
				}
			}
			catch(BluetoothException ble) {
				LOGGER.severe(String.format("%s.read: ERROR %s ... ",CLSS,ble.getLocalizedMessage()));
				text = "Error";
			}
			catch(InterruptedException ioe) {
				text = "Interrupted";
			}
		}

		return text;
	}
	
	
	/**
	 * Write plain text to the socket.
	 */
	public void write(String text) {
		if( characteristic!=null ) {
			characteristic.writeValue(text.getBytes());
		}
	}
	
	

	public class StringNotification implements BluetoothNotification<byte[]>  {
		
		public void run(byte[] bytes) {
			LOGGER.info(String.format("%s.StringNotification: Got one!", CLSS));
		}
	}
}

