/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.bert.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.UUID;

import chuckcoughlin.bert.common.MessageType;

/**
 *  This socket communicates across a Bluetooth network to the robot which acts
 *  as a server. The messages are simple text strings.
 *  
 *  The file descriptors are opened on "openConnections" and closed on
 *  "shutdown". Change listeners are notified (in a separate Thread) when the
 *  socket is "ready".
 */
public class BluetoothConnection {
	private static final String CLSS = "BluetoothConnection";
    private final BluetoothHandler handler;
    private ConnectionThread connectionThread = null;
	private ReaderThread readerThread = null;
	private static final int BUFFER_SIZE = 256;
	private static final long CLIENT_ATTEMPT_INTERVAL = 2000;  // 2 secs
	private static final int CLIENT_LOG_INTERVAL = 10;
	// Well-known port for Bluetooth serial port service
	private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private BluetoothDevice device;
	private BluetoothSocket socket;
	private BufferedReader in = null;
	private PrintWriter out =null;
	private final char[] buffer;

	/**
	 * Constructor: Use this version for processes that are clients
	 * @param handler the parent fragment
	 */
	public BluetoothConnection(BluetoothHandler handler) {
        this.buffer  = new char[BUFFER_SIZE];
		this.handler = handler;
	}


	public void openConnections(BluetoothDevice dev) {
	    this.device = dev;
        if( connectionThread!=null && connectionThread.isAlive() && !connectionThread.isInterrupted() ) {
			Log.i(CLSS, "socket connection already in progress ...");
			return;
		}
		connectionThread = new ConnectionThread(device);
        connectionThread.start();
	}

    public void stopChecking() {
        if (connectionThread != null && connectionThread.isAlive()) {
            connectionThread.interrupt();
        }
    }

	/**
	 * Close IO streams.
	 */
	public void shutdown() {
	    stopChecking();

		if(in!=null) {
			try{ in.close();} catch(IOException ignore) {}
			in = null;
		}
		if(out!=null) {
			out.close();
			out = null;
		}
		try {
			if( socket!=null) socket.close();
		}
		catch(IOException ioe) {}
			
	}

	/**
	 * Read a line of text from the socket. The read will block and wait for data to appear. 
	 * If we get a null, then close the socket and either re-open or re-listen
	 * depending on whether or not this is the server side, or not.
	 *
	 * @return either a RequestBottle or a ResponseBottle as appropriate.
	 */
	public String read() {
		String text = null;
		try {
			if(in!=null )  {
				Log.i(CLSS,String.format("read: reading ... "));
				int count = in.read(buffer,0,BUFFER_SIZE);
				Log.i(CLSS,String.format("read: got %d bytes !!",count));
			}
			else {
                Log.e(CLSS,String.format("read: Error reading from %s before connection",device.getName()));
			}
		}
		catch(IOException ioe) {
			Log.e(CLSS,String.format("read: Error reading from %s (%s)",device.getName(),ioe.getLocalizedMessage()));
			// Close and attempt to reopen port
			text = reread();
		}
		catch(NullPointerException npe) {
			Log.e(CLSS,String.format("read: Error reading from %s (%s)",device.getName(),npe.getLocalizedMessage()));
			// Close and attempt to reopen port
			text = reread();
		}
		return text;
	}

	/**
	 * Start a thread that loops forever in a blocking read.
	 */
	public void readInThread() {
		if( readerThread==null ) {
			readerThread = new ReaderThread();
			readerThread.start();
		}
	}
	
	/**
	 * Write plain text to the socket.
	 */
	public void write(String text) {
		String deviceName = (device==null?"No device":(device.getName()==null?"No name":device.getName()));
		try {
			if( out!=null ) {
				if(!out.checkError() ) {
					Log.i(CLSS, String.format("write: writing ... %s (%d bytes) to %s. ", text, text.length(), deviceName));
					out.print(text);
					out.flush();
				}
				else {
					Log.e(CLSS,String.format("write: out stream error",deviceName));
				}
			}
			else {
                Log.e(CLSS,String.format("write: Error writing to %s before connection",deviceName));
			}
		}
		catch(Exception ex) {
            Log.e(CLSS,String.format("write: Error writing %d bytes to %s(%s)",text.length(),deviceName,
												ex.getLocalizedMessage()),ex);
		}
	}
	
	/**
	 * We've gotten a null when reading the socket. This means, as far as I can tell, that the other end has 
	 * shut down. Close the socket and re-open or re-listen/accept. We hang until this succeeds.
	 * @return the next 
	 */
	private String reread() {
		String text = null;
        Log.i(CLSS,String.format("reread: on %s",device.getName()));
        shutdown();
        openConnections(device);
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Log.i(CLSS,String.format("reread: reopened %s for read",device.getName()));
			text = in.readLine();
		} 
		catch (Exception ex) {
            Log.i(CLSS,String.format("reread: ERROR opening %s for read (%s)",device.getName(),ex.getMessage()));
		}

        Log.i(CLSS,String.format("reread: got %s",text));
		return text;
	}

	// ================================================= Connection Thread =========================
	/**
	 * Check for the network in a separate thread.
	 */
	private class ConnectionThread extends Thread {
		private final BluetoothDevice device;

		public ConnectionThread(BluetoothDevice dev) {
			this.device = dev;
			setDaemon(true);
			// don't require callers to explicitly kill all the old checker threads.
			setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread thread, Throwable ex) {
					String msg = String.format("There was an uncaught exception creating socket connection: %s",ex.getLocalizedMessage());
					Log.e(CLSS,msg ,ex);
					handler.handleSocketError(msg);
				}
			});
		}

        /**
         * We are a client. Attempt to connect to the server.
         */
        @Override
        public void run() {
            String reason = null;

            // Keep attempting a connection until the server is ready
            int attempts = 0;
            boolean logged = false;
            for (;; ) {
                try {
                	UUID uuid = null;
					if( device.fetchUuidsWithSdp() ) {
						ParcelUuid[] uuids = device.getUuids();
						Log.i(CLSS, String.format("run: %s returned %d service UUIDs",device.getName(), uuids.length));
						for( ParcelUuid id:uuids) {
							uuid = id.getUuid();
							if(!logged) Log.i(CLSS, String.format("run: %s: service UUID = %s",device.getName(), uuid.toString()));
						}
						if( uuid==null ) {
							reason = String.format("There were no service UUIDs found on %s", device.getName());
							Log.w(CLSS, String.format("run: ERROR %s", reason));
							handler.handleSocketError(reason);
							break;
						}
						logged = true;
					}
					else {
						reason = String.format("The tablet failed to fetch service UUIDS to %s", device.getName());
						Log.w(CLSS, String.format("run: ERROR %s", reason));
						handler.handleSocketError(reason);
						break;
					}
					Log.i(CLSS, String.format("run: creating insecure RFComm socket for %s ...",SERIAL_UUID));
					socket = device.createInsecureRfcommSocketToServiceRecord(SERIAL_UUID);
					Log.i(CLSS, String.format("run: attempting to connect to %s ...", device.getName()));
					socket.connect();
                    Log.i(CLSS, String.format("run: connected to %s after %d attempts", device.getName(), attempts));
                    reason = openPorts();
                    break;
                }
                catch (IOException ioe) {
					Log.w(CLSS, String.format("run: IOException connecting to socket (%s)", ioe.getLocalizedMessage()));
                    // See: https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3
					try {
							Thread.sleep(CLIENT_ATTEMPT_INTERVAL);
					}
					catch (InterruptedException ie) {
						if (attempts % CLIENT_LOG_INTERVAL == 0) {
							reason = String.format("The tablet failed to create a client socket to %s due to %s", device.getName(), ioe.getMessage());
							Log.w(CLSS, String.format("run: ERROR %s", reason));
							handler.handleSocketError(reason);
						}
					}
                }
                attempts++;
            }
            if (reason==null) {
                handler.receiveSocketConnection();
            }
            else {
                handler.handleBluetoothError(reason);
            }
        }

        /**
         * Open IO streams for reading and writing. The socket must exist.
		 * @return an error description. Null if no error.
         */
        private String openPorts() {
            String reason = null;
            if( socket!=null ) {
                try {
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    Log.i(CLSS,String.format("openPorts: opened %s for read",device.getName()));
                    try {
                        out = new PrintWriter(socket.getOutputStream(),true);
                        Log.i(CLSS,String.format("openPorts: opened %s for write",device.getName()));
                        write(String.format("%s:the tablet is connected", MessageType.LOG.name()));
                    }
                    catch (Exception ex) {
                        reason = String.format("The tablet failed to open a socket for writing due to %s",ex.getMessage());
                        Log.i(CLSS,String.format("openPorts: ERROR opening %s for write (%s)",CLSS,device.getName(),ex.getMessage()),ex);
                        handler.handleSocketError(reason);
                    }
                }
                catch (Exception ex) {
                    reason = String.format("The tablet failed to open a socket for reading due to %s",ex.getMessage());
                    Log.i(CLSS,String.format("openPorts: ERROR opening %s for read (%s)",CLSS,device.getName(),ex.getMessage()),ex);
                    handler.handleSocketError(reason);
                }
            }
            return reason;
        }
	}
	// ================================================= Connection Thread =========================
	/**
	 * Check for the network in a separate thread.
	 */
	private class ReaderThread extends Thread {

		/**
		 * Read in a separate thread. Read blocks.
		 */
		@Override
		public void run() {
			for (; ; ) {
				try {

					read();
					Thread.sleep(100);
				}
				catch(InterruptedException ex) {

				}
			}
		}
	}
}
