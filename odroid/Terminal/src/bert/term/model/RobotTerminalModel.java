/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.term.model;

import java.nio.file.Path;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import bert.share.message.HandlerType;
import bert.share.model.AbstractRobotModel;
import bert.share.model.ConfigurationConstants;
import bert.share.xml.XMLUtility;

/**
 *  Read the XML configuration file to extract information needed by
 *  the Terminal application. 
 */
public class RobotTerminalModel extends AbstractRobotModel   {
	private static final String CLSS = "RobotTerminalModel";

	public RobotTerminalModel(Path configPath) {
		super(configPath);
	}
	
	/**
	 *  Analyze the document and populate the model. 
	 */
	@Override
	public void populate() {
		analyzeProperties();
		analyzeControllers();
	}


    // ================================ Auxiliary Methods  ===============================
	/**
	 * Search the XML configuration for the terminal controller. It's the only one we care about.
	 */
	@Override
	public void analyzeControllers() {
		if( this.document!=null ) {
			String controllerName = "UNASSIGNED";
			NodeList elements = document.getElementsByTagName("controller");
			int count = elements.getLength();
			int index = 0;
			while(index<count) {
				Element controllerElement= (Element)(elements.item(index));
				controllerName = XMLUtility.attributeValue(controllerElement, "name");
				String type = XMLUtility.attributeValue(controllerElement, "type");
				if( type!=null && !type.isEmpty() &&
					type.equalsIgnoreCase(HandlerType.TERMINAL.name()) ) {
					// Configure the socket - there should only be one.
					NodeList socketElements = controllerElement.getElementsByTagName("socket");
					if( socketElements.getLength()>0) {
						handlerTypes.put(controllerName,type.toUpperCase());
						Element socketElement= (Element)(socketElements.item(0));
						String portName = XMLUtility.attributeValue(socketElement, "port");
						sockets.put(controllerName,Integer.parseInt(portName));
					}
					break;
				}
				index++;
			}
			properties.put(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, controllerName);
		}
	}
}