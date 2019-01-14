/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.command.model;

import java.nio.file.Path;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bert.share.controller.ControllerType;
import bert.share.model.AbstractRobotModel;
import bert.share.xml.XMLUtility;

/**
 *  This is the base class for a collection of models that keep basic configuration
 *  information, all reading from the same files. The information 
 */
public class RobotCommandModel extends AbstractRobotModel   {
	private static final String CLSS = "RobotCommandModel";
	private static final System.Logger LOGGER = System.getLogger(CLSS);

	public RobotCommandModel(Path configPath) {
		super(configPath);
	}
    
	
	/**
	 *  Analyze the document and populate the model. 
	 */
	public void populate() {
		analyzeControllers();
		analyzeProperties();
		analyzeMotors();
	}


    // ================================ Auxiliary Methods  ===============================
	/**
	 * Search the model for the terminal controller. It's the only one we care about.
	 * If not found, the controller will be null.
	 */
	@Override
	public void analyzeControllers() {
		if( this.document!=null ) {
			NodeList elements = document.getElementsByTagName("controller");
			int count = elements.getLength();
			int index = 0;
			while(index<count) {
				Element controllerElement= (Element)(elements.item(index));
				String name = XMLUtility.attributeValue(controllerElement, "name");
				String type = XMLUtility.attributeValue(controllerElement, "type");
				if( type!=null && !type.isBlank() &&
					type.equalsIgnoreCase(ControllerType.COMMAND.name()) ) {
					// Configure the pipe - there should only be one.
					NodeList pipeElements = controllerElement.getElementsByTagName("pipe");
					if( pipeElements.getLength()>0) {
						Element pipeElement= (Element)(pipeElements.item(0));
						String pname = XMLUtility.attributeValue(pipeElement, "name");
						controllerTypes.put(name,type.toUpperCase());
						pipeNames.put(name,pname);
					}
					break;
				}
				
				index++;
			}
		}
	}
}

