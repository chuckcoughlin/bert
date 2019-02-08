/* **********************************************************************
 *   BoundedBuffer.java
 * **********************************************************************
 */
 package bert.share.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import bert.share.message.MessageHandler;

/**
 * Register this class with the Runtime to cleanup sockets on a 
 * CNTRL-C or other hang-up. 
 */
public class ShutdownHook implements Runnable {
	private static final String CLSS = "ShutdownHook";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private final MessageHandler msghandler;
	/**
     * Constructor: Create a new hook.
     */
    public ShutdownHook(MessageHandler mh) {
        this.msghandler = mh;
    }
    @Override
    public void run() {
    	LOGGER.info(String.format("%s: shutting down ...", CLSS));
    	try {
    		msghandler.stop();
    	}
    	catch(Exception ex) {
    		LOGGER.log(Level.SEVERE,String.format("%s: ERROR (%s, args)",CLSS,ex.getMessage()),ex);
    	}
      } 
}
