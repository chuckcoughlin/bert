/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.bert.service;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import chuckcoughlin.bert.common.BertConstants;
import chuckcoughlin.bert.db.SettingsManager;

/**
 *  This socket communicates across a Bluetooth network to the robot which acts
 *  as a server. The messages are simple text strings.
 *  
 *  The file descriptors are opened on "startup" and closed on 
 *  "shutdown". Change listeners are notified (in a separate Thread) when the
 *  socket is "ready".
 */
public class BluetoothSocket   {
	private static final String CLSS = "BluetoothSocket";
    private final VoiceServiceHandler handler;
    private boolean threadRunning;


	private static final long CLIENT_ATTEMPT_INTERVAL = 2000;  // 2 secs
	private static final int CLIENT_LOG_INTERVAL = 10;
	private final String host;
	private final int port;
	private Socket socket;
	private BufferedReader in = null;
	private PrintWriter out =null;

	/**
	 * Constructor: Use this version for processes that are clients
	 * @param handler the parent fragment
	 */
	public BluetoothSocket(VoiceServiceHandler handler) {
        this.threadRunning = false;
        this.handler = handler;
        this.host  = SettingsManager.getInstance().getSetting(BertConstants.BERT_SERVER);
		this.port =  Integer.parseInt(SettingsManager.getInstance().getSetting(BertConstants.BERT_PORT));
		this.socket = null;
	}

    /**
     * We are a client. Attempt to connect to the server.
     */
    public void create() {
        // Keep attempting a connection until the server is ready
        int attempts = 0;
        Log.i(CLSS,String.format("create: Attempting to connect to %s on %d",host,port));
        for(;;) {
            try  {
                socket = new Socket(host,port);
                Log.i(CLSS,String.format("create: connected to %s after %d attempts",host,attempts));
                if( startup() ) handler.receiveSocketConnection();
                break;
            }
            catch(IOException ioe) {
                try {
                    Thread.sleep(CLIENT_ATTEMPT_INTERVAL);
                }
                catch(InterruptedException ie) {
                    if( attempts%CLIENT_LOG_INTERVAL==0) {
                        String reason = String.format("The tablet failed to create a client socket to %s due to %s",host,ioe.getMessage());
                        Log.w(CLSS,String.format("create: ERROR %s", reason));
                        handler.handleSocketError(reason);
                    }
                }
            }
            attempts++;
        }
    }

	/**
	 * This must not be called before the socket is created.
	 * Open IO streams for reading and writing.
	 */
	private boolean startup() {
	    boolean success = false;
		if( socket!=null ) {
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				Log.i(CLSS,String.format("startup: opened %s (%d) for read",host,port));
                try {
                    out = new PrintWriter(socket.getOutputStream(),true);
                    Log.i(CLSS,String.format("startup: opened %s (%d) for write",host,port));
                    success = true;
                }
                catch (Exception ex) {
                    String reason = String.format("The tablet failed to open a socket for reading due to %s",ex.getMessage());
                    Log.i(CLSS,String.format("startup: ERROR opening port %d for write (%s)",CLSS,port,ex.getMessage()));
                    handler.handleSocketError(reason);
                }
			} 
			catch (Exception ex) {
                String reason = String.format("The tablet failed to open a socket for reading due to %s",ex.getMessage());
                Log.i(CLSS,String.format("startup: ERROR opening port %d for read (%s)",CLSS,port,ex.getMessage()));
                handler.handleSocketError(reason);
			}
		}
		return success;
	}
	
	/**
	 * Close IO streams.
	 */
	public void shutdown() {
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
                Log.e(CLSS,String.format("readLine: Error reading from %s before port is open",host));
			}
		}
		catch(IOException ioe) {
			Log.e(CLSS,String.format("readLine: Error reading from %s (%s)",host,ioe.getLocalizedMessage()));
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
				Log.i(CLSS,String.format("write: wrote %s %d bytes. ",CLSS,host,text.length()));
			}
			else {
                Log.e(CLSS,String.format("write: Error writing to %s before port is open",host));
			}
		}
		catch(Exception ioe) {
            Log.e(CLSS,String.format("write: Error writing %d bytes (%s)",text.length(), ioe.getLocalizedMessage()));
		}
	}
	
	/**
	 * We've gotten a null when reading the socket. This means, as far as I can tell, that the other end has 
	 * shut down. Close the socket and re-open or re-listen/accept. We hang until this succeeds.
	 * @return the next 
	 */
	private String reread() {
		String text = null;
        Log.i(CLSS,String.format("reread: on port %d",port));
		try{in.close();} catch(IOException ignore) {}
		try{socket.close();} catch(IOException ignore) {}
		create();
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Log.i(CLSS,String.format("reread: reopened %s (%d) for read",host,port));
			text = in.readLine();
		} 
		catch (Exception ex) {
            Log.i(CLSS,String.format("reread: ERROR opening %s for read (%s)",host,ex.getMessage()));
		}

        Log.i(CLSS,String.format("reread: got %s",text));
		return text;
	}
}

