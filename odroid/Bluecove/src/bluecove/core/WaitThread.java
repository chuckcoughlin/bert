
package bluecove.core;

import bc.javax.bluetooth.DiscoveryAgent;
import bc.javax.bluetooth.LocalDevice;
import bc.javax.bluetooth.UUID;
import bc.javax.microedition.io.Connector;
import bc.javax.microedition.io.StreamConnection;
import bc.javax.microedition.io.StreamConnectionNotifier;


public class WaitThread implements Runnable {

	/** Constructor */
	public WaitThread() {
	}

	@Override
	public void run() {
		waitForConnection();
	}

	/** Waiting for connection from devices */
	private void waitForConnection() {
		// retrieve the local Bluetooth device object
		LocalDevice local = null;

		StreamConnectionNotifier notifier;
		StreamConnection connection = null;

		// setup the server to listen for connection
		try {
			local = LocalDevice.getLocalDevice();
			local.setDiscoverable(DiscoveryAgent.GIAC);

			UUID uuid = new UUID("04c6093b00001000800000805f9b34fb", false);
			System.out.println(uuid.toString());
			String url = "btspp://localhost:" + uuid.toString() + ";name=RemoteBluetooth";
			notifier = (StreamConnectionNotifier)Connector.open(url);
		} 
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		// waiting for connection
		while(true) {
			try {
				System.out.println("waiting for connection...");
				connection = notifier.acceptAndOpen();

				Thread processThread = new Thread(new ProcessConnectionThread(connection));
				processThread.start();
			} 
			catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
}
