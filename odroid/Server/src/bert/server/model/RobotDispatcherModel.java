/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.server.model;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import bert.share.message.HandlerType;
import bert.share.model.AbstractRobotModel;
import bert.share.xml.XMLUtility;

/**
 *  The server-side model retains the configuration of all the request handlers
 *  plus a hand-full of properties. 
 */
public class RobotDispatcherModel extends AbstractRobotModel  {
	private static final String CLSS = "RobotDispatcherModel";
	private static final Logger LOGGER = Logger.getLogger(CLSS);;
			
	public RobotDispatcherModel(Path configPath) {
		super(configPath);
	}
   
	
	// Analyze the document
	public void populate() {
		analyzeProperties();
		analyzeControllers();
	}


    // ================================ Auxiliary Methods  ===============================
	/**
	 * Search the XML for the two command controllers (COMMAND and TERMINAL).
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
					NodeList socketElements = controllerElement.getElementsByTagName("socket");
					if( socketElements.getLength()>0) {
						handlerTypes.put(name,type.toUpperCase());
						Element socketElement= (Element)(socketElements.item(0));
						String portName = XMLUtility.attributeValue(socketElement, "port");
						sockets.put(name,Integer.parseInt(portName));
					}
				}
				else if( type!=null && !type.isBlank() &&
						type.equalsIgnoreCase(HandlerType.TERMINAL.name()) ) {
					NodeList socketElements = controllerElement.getElementsByTagName("socket");
					if( socketElements.getLength()>0) {
						handlerTypes.put(name,type.toUpperCase());
						Element socketElement= (Element)(socketElements.item(0));
						String portName = XMLUtility.attributeValue(socketElement, "port");
						sockets.put(name,Integer.parseInt(portName));
					}
				}
				
				index++;
			}
		}
	}
}
