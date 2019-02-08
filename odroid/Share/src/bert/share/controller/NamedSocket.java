/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import bert.share.message.MessageBottle;
/**
 *  The socket takes the name of the client. It encapsulates a bi-directional
 *  connection between client and server used for passing "RequestBottles" and 
 *  "ResponseBottles". The Server process should instantiate the with "server" = "true".
 *  
 *  The file descriptors are opened on "startup" and closed on 
 *  "shutdown". Change listeners are notified (in a separate Thread) when the
 *  socket is "ready".
 */
public class NamedSocket   {
	private static final String CLSS = "NamedSocket";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private static final long CLIENT_ATTEMPT_INTERVAL = 2000;  // 2 secs
	private static final int CLIENT_LOG_INTERVAL = 10;
	private final String name;
	private final String host;
	private final int port;
	private final boolean server;  // True if this instance is owned by the server.
	private ServerSocket serverSocket;
	private Socket socket;
	private BufferedReader in = null;
	private PrintWriter out =null;

	/**
	 * Constructor: Use this constructor from the server process.
	 * @param name identifier of the connection, the client name
	 * @param port communication port number
	 */
	public NamedSocket(String name,int port) {
		this.name = name;
		this.host = "";        // Not needed
		this.port = port;
		this.server = true;
		this.socket = null; 
		this.serverSocket = null;  
	}
	
	/**
	 * Constructor: Use this version for processes that are clients
	 * @param launcher the parent application
	 * @param hostname of the server process.
	 * @param port communication port number
	 */
	public NamedSocket(String name,String hostname,int port) {
		this.name = name;
		this.host = hostname; 
		this.port = port;
		this.server = false;
		this.socket = null;
		this.serverSocket = null;  // Not needed
	}
	
	public boolean isServer() {return this.server;}
	public String getName() {return this.name;}
	
	
	/**
	 * If we are a server, create a listener and wait to accept a connection.
	 * There is no action for a client.
	 */
	public boolean create() {
		boolean success = true;
		if( server ) {
		try  {
			serverSocket = new ServerSocket(port);
			LOGGER.info(String.format("%s.create: Server listening on port %d", CLSS,port));
			socket = serverSocket.accept();
			LOGGER.info(String.format("%s.create: Server accepted connection to %s", CLSS,name));
        }
		catch(Exception ex) {
			success = false;
			socket = null;
			LOGGER.severe(String.format("%s.create: ERROR creating server socket %s (%s)", CLSS,name,ex.getMessage()));
		}
		}
		else {
			// Keep attempting a connection until the server is ready
			int attempts = 0;
			LOGGER.info(String.format("%s.create: %s attempting to connect to server ...", CLSS,name));
			for(;;) {
				try  {
					socket = new Socket(host,port);
					LOGGER.info(String.format("%s.create: new server connection from %s after %d attempts", CLSS,name,attempts));
					break;
				}
				catch(IOException ioe) {
					try {
						Thread.sleep(CLIENT_ATTEMPT_INTERVAL);
					}
					catch(InterruptedException ie) {
						if( attempts%CLIENT_LOG_INTERVAL==0) {
							LOGGER.warning(String.format("%s.create: ERROR creating client socket %s (%s)", CLSS,name,ioe.getMessage()));
						}
					}

				}
				attempts++;
			}
		}
		return success;
	}

	/**
	 * This must not be called before the socket is created.
	 * Open IO streams for reading and writing.
	 */
	public void startup() {
		if( socket!=null ) {
			try {
				LOGGER.info(String.format("%s.startup: opening %s for read",CLSS,name));
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				LOGGER.info(String.format("%s.startup: opened %s for read",CLSS,name));
			} 
			catch (Exception ex) {
				LOGGER.info(String.format("%s.startup: ERROR opening %s for read (%s)",CLSS,name,ex.getMessage()));
			} 
			try {
				LOGGER.info(String.format("%s.startup: opening %s for write",CLSS,name));
				out = new PrintWriter(socket.getOutputStream(),true);
				LOGGER.info(String.format("%s.startup: opened %s for write",CLSS,name));
			} 
			catch (Exception ex) {
				LOGGER.info(String.format("%s.startup: ERROR opening %s for write (%s)",CLSS,name,ex.getMessage()));
			}
		}
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
			if(serverSocket!=null) serverSocket.close();
		}
		catch(IOException ioe) {}
			
	}
	/**
	 * Read from the pipe that is appropriate, depending on the ownership. The read will 
	 * block and wait for data to appear on the pipe. 
	 *
	 * @return either a RequestBottle or a ResponseBottle as appropriate.
	 */
	public MessageBottle read() {
		MessageBottle bottle = null;
		try {
			if(in!=null )  {
				LOGGER.info(String.format("%s.read: reading %s ... ",CLSS,name));
				String json = in.readLine();
				LOGGER.info(String.format("%s.read: got %s",CLSS,json));
				if( json!=null) bottle = MessageBottle.fromJSON(json);
			}
			else {
				LOGGER.severe(String.format("%s.read: Error reading from %s before port is open",CLSS,name));
			}
		}
		catch(IOException ioe) {
			LOGGER.severe(String.format("%s.read: Error reading from %s (%s)",CLSS,name,ioe.getLocalizedMessage()));
		}
		return bottle;
	}
	
	
	/**
	 * Write the serialized MessageBottle to the socket. Include a 4 byte prefix that is the length of the 
	 * JSON string to follow. Include the null string termination in the byte count.
	 */
	public void write(MessageBottle bottle) {
		String json = bottle.toJSON();
		//byte[] bytes = json.getBytes();
		//int size = bytes.length;
		try {
			if( out!=null ) {
				LOGGER.info(String.format("%s.write: writing %s %d bytes... ",CLSS,name,json.length()));
				out.println(json);
				out.flush();
				LOGGER.info(String.format("%s.write: complete. ",CLSS,name));
			}
			else {
				LOGGER.severe(String.format("%s.write: Error writing to %s before port is open",CLSS,name));
			}
		}
		catch(Exception ioe) {
			LOGGER.severe(String.format("%s.write: Error writing %d bytes (%s)",CLSS,json.length(), ioe.getLocalizedMessage()));
		}
	}
}

