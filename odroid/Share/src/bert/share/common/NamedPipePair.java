/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.common;

import java.io.File;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;

import bert.share.bottle.BasicBottle;
/**
 *  This class encapsulates two named pipes for bi-directional
 *  communication. The "server" or "owner" is taken to be the 
 *  Dispatcher process. All others should instantiate with "false".
 */
public class NamedPipePair   {
	private static final String CLSS = "NamedPipePair";
	public final String FROM_SERVER = "_fromServer";
	public final String TO_SERVER   = "_toServer";
	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private final boolean owner;  // True if this instance is owned by the server.
	private boolean useAsynchronousReads;
	private String name;
	private BufferedReader bufferedReader;
	
	public NamedPipePair(boolean isOwner) {
		String name = "";
		this.owner = isOwner;
		this.useAsynchronousReads = false;
		this.bufferedReader = null;
	}
	
	public boolean isOwner() {return this.owner;}
	public String getName() {return this.name;}
 	public void setName(String nam) {this.name=nam;}
 	public void setReadsAsynchronous(boolean flag) {
 		this.useAsynchronousReads = flag;
 		if( useAsynchronousReads ) {
 			bufferedReader = new BufferedReader();
 			Thread thread = new Thread(bufferedReader);
 			thread.start();
 		}
 		else {
 			bufferedReader.stop();
 			bufferedReader = null;
 		}
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
	 * Read from the pipe that is appropriate, depending on the ownership. Depending
	 * on the "useAsynchronousReads" flag, the read may wait for data to appear on
	 * the pipe, or return immediately with either data already present or a null. 
	 *
	 * @return either a RequestBottle or a ResponseBottle as appropriate.
	 */
	public BasicBottle read() {
		if( owner ) {
			
		}
		else {
			
		}
		return null;
	}
	
	
	/**
	 * Write to the pipe that is appropriate, depending on the ownership. This is a 
	 * synchronous operation.
	 */
	public void write(BasicBottle data) {
		if( owner ) {
			
		}
		else {
			
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
		String name = root+FROM_SERVER;
		Path fifoPath = Paths.get(parent.toString(), name);
	    String[] command = new String[] {"mkfifo", fifoPath.toString()};
	    try {
	    	Process process = new ProcessBuilder(command).inheritIO().start();
	    	process.waitFor();
	    }
	    catch(Exception ex) {
	    	success = false;
	    	LOGGER.log(Level.ERROR,String.format("%s.createFifoPipe: Exception (%s) creating %s",CLSS,ex.getLocalizedMessage(),fifoPath.toString()));
	    }
	    // Create sibling for opposite direction
	    name = root+TO_SERVER;
		fifoPath = Paths.get(parent.toString(), name);
	    command = new String[] {"mkfifo", fifoPath.toString()};
	    try {
	    	Process process = new ProcessBuilder(command).inheritIO().start();
	    	process.waitFor();
	    }
	    catch(Exception ex) {
	    	success = false;
	    	LOGGER.log(Level.ERROR,String.format("%s.createFifoPipe: Exception (%s) creating %s",CLSS,ex.getLocalizedMessage(),fifoPath.toString()));
	    }
	    return success;
	}
	
	/**
	 * This nested class is the runnable with a bounded buffer that allows us to do an asynchronous read.
	 *
	 */
	public class BufferedReader implements Runnable {
		private boolean stopped;
		public BufferedReader() {
			this.stopped = false;
		}
		
		public void stop() { this.stopped = true; }
		
		public void run() {
			while( !stopped ) {
				
			}
		}
	}
	
}

