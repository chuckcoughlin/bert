/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.model;

import java.nio.file.Path;
import java.util.logging.Logger;

import bert.share.model.AbstractRobotModel;

/**
 *  This model is used exclusively for testing.
 */
public class TestRobotModel extends AbstractRobotModel  {
	private static final String CLSS = "TestRobotModel";
	private static final Logger LOGGER = Logger.getLogger(CLSS);

	
	public TestRobotModel(Path configPath) {
		super(configPath);
	}
    

	/**
	 *  No need for controllers in our test 
	 */
	public void analyzeControllers() {}
	
	/**
	 *  Analyze the document. The information retained is dependent on the context
	 *  (client or server). This must be called before the model is accessed.
	 */
	public void populate() {
		analyzeProperties();
		analyzeMotors();
	}
}


