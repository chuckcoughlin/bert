/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.term.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.nio.file.Paths;

import bert.share.bottle.BottleConstants;
import bert.share.controller.AbstractController;
import bert.share.controller.Controller;
import bert.share.controller.ControllerLauncher;
import bert.share.model.AbstractRobotModel;
import bert.share.model.NamedPipePair;
import bert.share.util.BoundedBuffer;
import bert.term.model.RobotTerminalModel;


/**
 * "Terminal" is an application that allows interaction from the command line
 * to command and interrogate the robot. Command entries are the same as
 * those given to the "headless" application, "Bert" in spoken form.
 */
public class TerminalController extends AbstractController implements Controller, Runnable {
	private final static String CLSS = "Terminal";
	private static final String USAGE = "Usage: terminal <config-file>";
	private System.Logger LOGGER = System.getLogger(CLSS);
	private final int BUFFER_SIZE = 5;
	private RobotTerminalModel model = null;
	private NamedPipePair pipe = null;
	private String prompt;
	private BoundedBuffer buffer;
	
	public TerminalController(String key,ControllerLauncher launcher) {
		super(key,launcher);
		this.buffer = new BoundedBuffer(BUFFER_SIZE);
		this.prompt = model.getProperty(BottleConstants.PROPERTY_PROMPT,"bert:");
	}
	
	@Override
	public void configure(AbstractRobotModel absModel) {
		if( absModel instanceof RobotTerminalModel ) {
			this.model = (RobotTerminalModel)absModel;
			setPipe(model.getPipe());
		}
		else {
			LOGGER.log(Level.ERROR,String.format("%s.configure: Wrong class (%s) given to method",CLSS,absModel.getClass().getCanonicalName()));
		}
	}
	
	public BoundedBuffer getClientToControllerBuffer() { return this.buffer; } 
	
	/**
	 * @param p named pipe pair that handles two-way I/O with server
	 */
	@Override
	public void setPipe(NamedPipePair p) {this.pipe = p;}
	
	/**
	 * On receipt of a command,
	 *   1) Read from input buffer 
	 *   2) Write to named pipe
	 *   3) Read response from named pipe
	 *   4) Invoke callback method on launcher.
	 */
	public void run() {
		BufferedReader br = null;
		try {

			Thread.sleep(1);

		} 
		catch(InterruptedException inte) {}
 
		finally {
			if (br != null) {
				try {
					br.close();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		System.exit(0);
	}

}
