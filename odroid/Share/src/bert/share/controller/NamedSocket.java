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
 *  
 *  NOTE: setting a timeout on normal socket operations like accept() is
 *        a bad idea. Next time around we end up with a "socket in use error".
 */
public class NamedSocket   {
	private static final String CLSS = "NamedSocket";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private static final long CLIENT_ATTEMPT_INTERVAL = 2000;  // 2 secs
	private static final int CLIENT_LOG_INTERVAL = 10;
	private static final long SERVER_ATTEMPT_INTERVAL = 15000;  // 15 secs
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
		int attempts = 1;
		if( server ) {
			for(;;) {
				try  {
					serverSocket = new ServerSocket(port);
					LOGGER.info(String.format("%s.create: %s as server listening on port %d", CLSS,name,port));
					socket = serverSocket.accept();
					LOGGER.info(String.format("%s.create: %s accepted connection on port %d after %d attempts", CLSS,name,port,attempts));
					break;
					
				}
				catch(Exception ex) {
					success = false;
					socket = null;
					LOGGER.severe(String.format("%s.create: ERROR creating server socket %s (%s)", CLSS,name,ex.getMessage()));
					try { 
						Thread.sleep(SERVER_ATTEMPT_INTERVAL);  // Something bad has happened, we don't want a hard loop
					}
					catch(InterruptedException ignore) {}
				}
				attempts++;
			}
		}
		else {
			// Keep attempting a connection until the server is ready
			
			for(;;) {
				try  {
					LOGGER.info(String.format("%s.create: %s attempting to connect to server %s on %d ...", CLSS,name,host,port));
					socket = new Socket(host,port);
					LOGGER.info(String.format("%s.create: new %s connection from %s on %d after %d attempts", CLSS,name,host,port,attempts));
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
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				LOGGER.info(String.format("%s.startup: opened %s for read",CLSS,name));
			} 
			catch (Exception ex) {
				LOGGER.info(String.format("%s.startup: ERROR opening %s for read (%s)",CLSS,name,ex.getMessage()));
			} 
			try {
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
		LOGGER.info(String.format("%s.shutdown: %s ... ",CLSS,name));
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
	 * Read from the socket. The read will block and wait for data to appear. 
	 * If we get a null, then close the socket and either re-open or re-listen
	 * depending on whether or not this is the server side, or not.
	 *
	 * @return either a RequestBottle or a ResponseBottle as appropriate.
	 */
	public MessageBottle read() {
		MessageBottle bottle = null;
		try {
			if(in!=null )  {
				LOGGER.info(String.format("%s.read: reading %s ... ",CLSS,name));
				String json = in.readLine();
				while( json==null ) {
					json = reread();
				}
				LOGGER.info(String.format("%s.read: got %s",CLSS,json));
				if( json!=null) bottle = MessageBottle.fromJSON(json);
			}
			else {
				LOGGER.warning(String.format("%s.read: Attempt to read from %s before port is open (ignored)",CLSS,name));
			}
		}
		catch(IOException ioe) {
			LOGGER.severe(String.format("%s.read: Exception reading from %s (%s)",CLSS,name,ioe.getLocalizedMessage()));
		}
		return bottle;
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
				LOGGER.info(String.format("%s.readLine: reading %s ... ",CLSS,name));
				text = in.readLine();
				while( text==null ) {
					text = reread();
				}
				LOGGER.info(String.format("%s.readLine: got %s",CLSS,text));
			}
			else {
				LOGGER.warning(String.format("%s.readLine: Attempt to read from %s before port is open )ignored)",CLSS,name));
			}
		}
		catch(IOException ioe) {
			LOGGER.severe(String.format("%s.readLine: Exception reading from %s (%s)",CLSS,name,ioe.getLocalizedMessage()));
		}
		return text;
	}
	
	/**
	 * Write the MessageBottle serialized as a JSON string to the socket.
	 */
	public void write(MessageBottle bottle) {
		String json = bottle.toJSON();
		//byte[] bytes = json.getBytes();
		//int size = bytes.length;
		try {
			if( out!=null ) {
				out.println(json);
				out.flush();
				LOGGER.info(String.format("%s.write: wrote %s %d bytes. ",CLSS,name,json.length()));
			}
			else {
				LOGGER.warning(String.format("%s.write: Attempt to write to %s before port is open (ignored)",CLSS,name));
			}
		}
		catch(Exception ioe) {
			LOGGER.severe(String.format("%s.write: Exception writing %d bytes (%s)",CLSS,json.length(), ioe.getLocalizedMessage()));
		}
	}
	
	/**
	 * Write plain text to the socket.
	 */
	public void write(String text) {
		try {
			if( out!=null ) {
				out.println(text);
				out.flush();
				LOGGER.info(String.format("%s.write: wrote %s %d bytes. ",CLSS,name,text.length()));
			}
			else {
				LOGGER.warning(String.format("%s.write: Attempt to write to %s before port is open (ignored)",CLSS,name));
			}
		}
		catch(Exception ioe) {
			LOGGER.severe(String.format("%s.write: Exception writing %d bytes (%s)",CLSS,text.length(), ioe.getLocalizedMessage()));
		}
	}
	
	/**
	 * We've gotten a null when reading the socket. This means, as far as I can tell, that the other end has 
	 * shut down. Close the socket and re-open or re-listen/accept. We hang until this succeeds.
	 * @return the next 
	 */
	private String reread() {
		String json = null;
		LOGGER.info(String.format("%s.reread: on port %s",CLSS,name));
		if(in!=null)             try{in.close();} catch(IOException ignore) {}
		if(socket!=null)         try{socket.close();} catch(IOException ignore) {}
		if( serverSocket!=null ) try{serverSocket.close();} catch(IOException ignore) {}
		create();
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			LOGGER.info(String.format("%s.reread: reopened %s for read",CLSS,name));
			json = in.readLine();
		} 
		catch (Exception ex) {
			LOGGER.info(String.format("%s.reread: ERROR opening %s for read (%s)",CLSS,name,ex.getMessage()));
		} 
		
		LOGGER.info(String.format("%s.reread: got %s",CLSS,json));
		return json;
	}
}

