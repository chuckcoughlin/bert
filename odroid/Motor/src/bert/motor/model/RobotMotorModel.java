/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.model;

import java.nio.file.Path;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bert.share.bottle.BottleConstants;
import bert.share.common.PathConstants;
import bert.share.model.AbstractRobotModel;
import bert.share.xml.XMLUtility;

/**
 *  The server-side model retains the configuration of all the request handlers
 *  plus a hand-full of properties. 
 */
public class RobotMotorModel extends AbstractRobotModel  {
	private static final String CLSS = "RobotServerModel";
	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private static final System.Logger.Level level = System.Logger.Level.WARNING;
	
	public RobotMotorModel(Path configPath) {
		super(configPath);

	}
   
	
	// Analyze the document
	public void populate() {
		analyzeProperties();
	}

    // ================================ Auxiliary Methods  ===============================

}
