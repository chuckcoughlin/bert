/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.bert.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.UUID;

import chuckcoughlin.bert.common.BertConstants;
import chuckcoughlin.bert.db.SettingsManager;

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
    private final VoiceServiceHandler handler;
    private ConnectionThread connectionThread = null;

	private static final long CLIENT_ATTEMPT_INTERVAL = 2000;  // 2 secs
	private static final int CLIENT_LOG_INTERVAL = 10;
    private static final String DEVICE_UUID = "33001101-0000-2000-8080-00815FAB34FF";
	private BluetoothDevice device;
	private BluetoothSocket socket;
	private BufferedReader in = null;
	private PrintWriter out =null;

	/**
	 * Constructor: Use this version for processes that are clients
	 * @param handler the parent fragment
	 */
	public BluetoothConnection(VoiceServiceHandler handler) {
        this.handler = handler;
	}


	public void openConnections(BluetoothDevice device) {
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
	public String readLine() {
		String text = null;
		try {
			if(in!=null )  {
				Log.i(CLSS,String.format("readLine: reading ... "));
				text = in.readLine();
				while( text==null ) {
					text = reread();
				}
				Log.i(CLSS,String.format("readLine: got %s",text));
			}
			else {
                Log.e(CLSS,String.format("readLine: Error reading from %s before connection",device.getName()));
			}
		}
		catch(IOException ioe) {
			Log.e(CLSS,String.format("readLine: Error reading from %s (%s)",device.getName(),ioe.getLocalizedMessage()));
		}
		return text;
	}

	
	/**
	 * Write plain text to the socket.
	 */
	public void write(String text) {
		try {
			if( out!=null ) {
				out.println(text);
				out.flush();
				Log.i(CLSS,String.format("write: wrote %d bytes to %s. ",CLSS,text.length(),device.getName()));
			}
			else {
                Log.e(CLSS,String.format("write: Error writing to %s before connection",device.getName()));
			}
		}
		catch(Exception ioe) {
            Log.e(CLSS,String.format("write: Error writing %d bytes to %s(%s)",text.length(),device.getName(),ioe.getLocalizedMessage()));
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
            for (;; ) {
                try {
					Log.i(CLSS, String.format("create: Attempting to connect to %s", device.getName()));
                    UUID uuid = UUID.fromString(DEVICE_UUID);
					socket = device.createRfcommSocketToServiceRecord(uuid);
                    Log.i(CLSS, String.format("create: connected to %s after %d attempts", device.getName(), attempts));
                    reason = openPorts();
                    break;
                }
                catch (IOException ioe) {
                    try {
                        Thread.sleep(CLIENT_ATTEMPT_INTERVAL);
                    }

                    catch (InterruptedException ie) {
                        if (attempts % CLIENT_LOG_INTERVAL == 0) {
                            reason = String.format("The tablet failed to create a client socket to %s due to %s", device.getName(), ioe.getMessage());
                            Log.w(CLSS, String.format("create: ERROR %s", reason));
                            handler.handleSocketError(reason);
                        }
                    }
                }
                attempts++;
            }
            if (reason==null) {
                handler.receiveSocketConnection();
            } else {
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
                    Log.i(CLSS,String.format("startup: opened %s for read",device.getName()));
                    try {
                        out = new PrintWriter(socket.getOutputStream(),true);
                        Log.i(CLSS,String.format("startup: opened %s for write",device.getName()));
                    }
                    catch (Exception ex) {
                        reason = String.format("The tablet failed to open a socket for reading due to %s",ex.getMessage());
                        Log.i(CLSS,String.format("startup: ERROR opening %s for write (%s)",CLSS,device.getName(),ex.getMessage()));
                        handler.handleSocketError(reason);
                    }
                }
                catch (Exception ex) {
                    reason = String.format("The tablet failed to open a socket for reading due to %s",ex.getMessage());
                    Log.i(CLSS,String.format("startup: ERROR opening %s for read (%s)",CLSS,device.getName(),ex.getMessage()));
                    handler.handleSocketError(reason);
                }
            }
            return reason;
        }
	}
}
