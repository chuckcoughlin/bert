/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.controller;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import bert.share.message.MessageBottle;
/**
 *  The socket takes the name of the client. It encapsulates a bi-directional
 *  connection between client and server used for passing "RequestBottles" and 
 *  "ResponseBottles". The Server process should instantiate the with "server" = "true".
 *  
 *  The file descriptors are opened on "startup" and closed on 
 *  "shutdown".
 */
public class NamedSocket   {
	private static final String CLSS = "NamedSocket";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private final String name;
	private final String host;
	private final int port;
	private final boolean server;  // True if this instance is owned by the server.
	private ServerSocket serverSocket;
	private Socket socket;
	private BufferedReader in = null;
	private BufferedOutputStream out =null;

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
			socket = serverSocket.accept();
			LOGGER.info(String.format("%s.create: %s accepted connection", CLSS,name));
        }
		catch(Exception ex) {
			success = false;
			LOGGER.severe(String.format("%s.create: ERROR creating server socket %s (%s)", CLSS,name,ex.getLocalizedMessage()));
		}
		}
		else {
			try  {
				socket = new Socket(host,port);
				LOGGER.info(String.format("%s.create: new connection %s", CLSS,name));
	        }
			catch(Exception ex) {
				success = false;
				LOGGER.severe(String.format("%s.create: ERROR creating client socket %s (%s)", CLSS,name,ex.getLocalizedMessage()));
			}
		}
	    return success;
	}
	
	/**
	 * This must not be called before the socket is created.
	 * Open IO streams for reading and writing.
	 */
	public void startup() {
		try {
			LOGGER.info(String.format("%s.startup: opening %s for read",CLSS,name));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            LOGGER.info(String.format("%s.startup: opened %s for read",CLSS,name));
        } 
		catch (Exception ex) {
			LOGGER.info(String.format("%s.startup: ERROR opening %s for read (%s)",CLSS,name,ex.getLocalizedMessage()));
        } 
		try {
			LOGGER.info(String.format("%s.startup: opening %s for write",CLSS,name));
            out = new BufferedOutputStream(socket.getOutputStream());
            LOGGER.info(String.format("%s.startup: opened %s for write",CLSS,name));
        } 
		catch (Exception ex) {
			LOGGER.info(String.format("%s.startup: ERROR opening %s for write (%s)",CLSS,name,ex.getLocalizedMessage()));
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
			try{ out.close();} catch(IOException ignore) {}
			out = null;
		}
		try {
			socket.close();
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
	 * Write to the pipe that is appropriate. With named pipes this is guaranteed
	 * to be an atomic operation as long as size is less than PIPE_SYNCH (and it is).
	 */
	public void write(MessageBottle bottle) {
		String json = bottle.toJSON();
		byte[] bytes = json.getBytes();
		try {
			if( out!=null ) {
				LOGGER.info(String.format("%s.write: writing %s %d bytes... ",CLSS,name,bytes.length));
				out.write(bytes,0,bytes.length);
				out.flush();
			}
			else {
				LOGGER.severe(String.format("%s.write: Error writing to %s before port is open",CLSS,name));
			}
		}
		catch(IOException ioe) {
			LOGGER.severe(String.format("%s.write: Error writing %d bytes (%s)",CLSS,bytes.length, ioe.getLocalizedMessage()));
		}
	}
}

