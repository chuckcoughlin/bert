/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
 package bert.share.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import bert.share.message.MessageHandler;

/**
 * Register this class with the Runtime to cleanup sockets on a 
 * SIGTERM, SIGINT, or SIGHUP. We do a hard shutdown.
 * 
 * It appears as if the logging is in-effective here.
 */
public class ShutdownHook extends Thread {
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
    		msghandler.shutdown();
    	}
    	catch(Exception ex) {
    		LOGGER.log(Level.SEVERE,String.format("%s: ERROR (%s, args)",CLSS,ex.getMessage()),ex);
    	}
    	Runtime.getRuntime().halt(0);
      } 
}
