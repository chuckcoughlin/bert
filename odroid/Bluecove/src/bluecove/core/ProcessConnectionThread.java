package bluecove.core;

import java.io.InputStream;

import bc.javax.microedition.io.StreamConnection;

public class ProcessConnectionThread implements Runnable {
private StreamConnection mConnection;
	
	// Constants that indicate commands from devices
	private static final int EXIT_CMD = -1;
	private static final int KEY_RIGHT = 1;
	private static final int KEY_LEFT = 2;
	
	public ProcessConnectionThread(StreamConnection connection) {
		mConnection = connection;
	}
	
	@Override
	public void run() {
		try {
			
			// prepare to receive data
			InputStream inputStream = mConnection.openInputStream();
	        
			System.out.println("waiting for input");
	        
	        while (true) {
	        	int command = inputStream.read();
	        	
	        	if (command == EXIT_CMD)
	        	{	
	        		System.out.println("finish process");
	        		break;
	        	}
	        	
	        	processCommand(command);
        	}
        } catch (Exception e) {
    		e.printStackTrace();
    	}
	}
	
	/**
	 * Process the command from client
	 * @param command the command code
	 */
	private void processCommand(int command) {
		try {
		
			switch (command) {
	    	case KEY_RIGHT:
	    		System.out.println("Right");
	    		break;
	    	case KEY_LEFT:
	    		System.out.println("Left");
	    		break;
	    	default:
	    		System.out.println("Received unknown command");
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
}
}
