/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.common;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import bert.share.bottle.MessageBottle;
/**
 *  This class encapsulates two named pipes for bi-directional
 *  communication. The "server" or "owner" is taken to be the 
 *  Dispatcher process. All others should instantiate with "false".
 *  
 *  The file descriptors are opened on "startup" and closed on 
 *  "shutdown".
 */
public class NamedPipePair   {
	private static final String CLSS = "NamedPipePair";
	public static final String FROM_DISPATCHER = "_fromDispatcher";
	public static final String TO_DISPATCHER   = "_toDispatcher";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private final String name;
	private final boolean owner;  // True if this instance is owned by the server.
	private boolean useAsynchronousReads;
	private String pathToRead="";
	private String pathToWrite="";
	private BufferedReader in = null;
	private BufferedOutputStream out =null;

	/**
	 * Constructor
	 * @param name root name of the pipe-pair.
	 * @param isOwner the Dispatcher "owns" the pipes.
	 */
	public NamedPipePair(String name,boolean isOwner) {
		this.name = name;
		this.owner = isOwner;
		this.useAsynchronousReads = false;
		initialize();
	}
	
	public boolean isOwner() {return this.owner;}
	public String getName() {return this.name;}
 	private void initialize() {
 		Path parent = PathConstants.DEV_DIR; 
 		String fname = name+TO_DISPATCHER;
 		if(owner) {
			this.pathToRead = Paths.get(parent.toString(), fname).toString();
		}
		else {
			this.pathToWrite = Paths.get(parent.toString(), fname).toString();
		}
 		
 		fname = name+FROM_DISPATCHER;
 		if(owner) {
			this.pathToWrite = Paths.get(parent.toString(), fname).toString();
		}
		else {
			this.pathToRead = Paths.get(parent.toString(), fname).toString();
		}
 	}
 	public void setReadsAsynchronous(boolean flag) {
 		this.useAsynchronousReads = flag;
 	}
	
	/**
	 * Create the pair of named pipes if they don't already exist.
	 * By convention, it is the server-side that does the creating.
	 * @return true if the pipes are ready for reading and writing.
	 */
	public boolean create() {
		Path dev = PathConstants.DEV_DIR;  // Parent directory for pipes
		File dir = new File(dev.toString());
	    if (! dir.exists()){
	        dir.mkdir();
	    }
	    boolean result = createFifoPipe(dir,name);
	    return result;
	}
	
	/**
	 * This must not be called before the pipes are created and named.
	 * Open IO streams for reading and writing.
	 */
	public void startup() {
		try {
			FileReader freader = new FileReader(pathToRead);
			in = new BufferedReader(freader);
		}
		catch(IOException ioe) {
			LOGGER.severe(String.format("%s.startup: Error opening %s for read (%s)",CLSS,pathToRead,ioe.getLocalizedMessage()));
		}
		
		try {
			out = new BufferedOutputStream(new FileOutputStream(pathToWrite));
		}
		catch(IOException ioe) {
			LOGGER.severe(String.format("%s.startup: Error opening %s for write (%s)",CLSS,pathToWrite,ioe.getLocalizedMessage()));
		}
	}
	
	/**
	 * Close IO streams.
	 */
	public void shutdown() {
		if(in!=null) {
			try{ in.close();} catch(IOException ignore) {}
		}
		if(out!=null) {
			try{ out.close();} catch(IOException ignore) {}
		}
	}
	/**
	 * Read from the pipe that is appropriate, depending on the ownership. Depending
	 * on the "useAsynchronousReads" flag, the read may wait for data to appear on
	 * the pipe, or return immediately with either data already present or a null. 
	 *
	 * @return either a RequestBottle or a ResponseBottle as appropriate.
	 */
	public MessageBottle read() {
		MessageBottle bottle = null;
		try {
			if(!useAsynchronousReads||in.ready()) {
				String json = in.readLine();
				bottle = MessageBottle.fromJSON(json);
			}
		}
		catch(IOException ioe) {
			LOGGER.severe(String.format("%s.read: Error reading from %s (%s)",CLSS,pathToRead,ioe.getLocalizedMessage()));
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
			out.write(bytes,0,bytes.length);
			out.flush();
		}
		catch(IOException ioe) {
			LOGGER.severe(String.format("%s.write: Error writing %d bytes (%s)",CLSS,bytes.length, ioe.getLocalizedMessage()));
		}
	}
	
	/**
	 * Create a pair of named pipes. The names are derived from
	 * the root name as "to" and "from" from the point-of-view of
	 * the Dispatcher process.
	 * 
	 * @param parent directory for the fifo files
	 * @param root name of the pipe pair before adding suffices
	 */
	private boolean createFifoPipe(File parent,String root)  {
		boolean success = true;
		String name = root+FROM_DISPATCHER;
		Path fifoPath = Paths.get(parent.toString(), name);
	    String[] command = new String[] {"mkfifo", fifoPath.toString()};
	    try {
	    	Process process = new ProcessBuilder(command).inheritIO().start();
	    	process.waitFor();
	    }
	    catch(Exception ex) {
	    	success = false;
	    	LOGGER.severe(String.format("%s.createFifoPipe: Exception (%s) creating %s",CLSS,ex.getLocalizedMessage(),fifoPath.toString()));
	    }
	    // Create sibling for opposite direction
	    name = root+TO_DISPATCHER;
		fifoPath = Paths.get(parent.toString(), name);
	    command = new String[] {"mkfifo", fifoPath.toString()};
	    try {
	    	Process process = new ProcessBuilder(command).inheritIO().start();
	    	process.waitFor();
	    }
	    catch(Exception ex) {
	    	success = false;
	    	LOGGER.severe(String.format("%s.createFifoPipe: Exception (%s) creating %s",CLSS,ex.getLocalizedMessage(),fifoPath.toString()));
	    }
	    return success;
	}
	
}

