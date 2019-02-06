/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.command.model;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import bert.share.message.HandlerType;
import bert.share.model.AbstractRobotModel;
import bert.share.xml.XMLUtility;

/**
 *  This is the base class for a collection of models that keep basic configuration
 *  information, all reading from the same files. The information 
 */
public class RobotCommandModel extends AbstractRobotModel   {
	private static final String CLSS = "RobotCommandModel";
	private static final Logger LOGGER = Logger.getLogger(CLSS);

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
	 * Search the XML configuration for the command controller. It's the only one we care about.
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
					type.equalsIgnoreCase(HandlerType.COMMAND.name()) ) {
					// Configure the socket - there should only be one.
					NodeList socketElements = controllerElement.getElementsByTagName("socket");
					if( socketElements.getLength()>0) {
						handlerTypes.put(name,type.toUpperCase());
						Element socketElement= (Element)(socketElements.item(0));
						String portName = XMLUtility.attributeValue(socketElement, "port");
						sockets.put(name,Integer.parseInt(portName));
					}
					break;
				}
				
				index++;
			}
		}
	}
}

