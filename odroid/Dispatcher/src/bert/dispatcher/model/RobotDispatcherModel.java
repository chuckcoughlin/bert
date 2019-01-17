/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.dispatcher.model;

import java.nio.file.Path;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import bert.share.controller.ControllerType;
import bert.share.model.AbstractRobotModel;
import bert.share.xml.XMLUtility;

/**
 *  The server-side model retains the configuration of all the request handlers
 *  plus a hand-full of properties. 
 */
public class RobotDispatcherModel extends AbstractRobotModel  {
	private static final String CLSS = "RobotDispatcherModel";
	private static final System.Logger LOGGER = System.getLogger(CLSS);;
			
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
					type.equalsIgnoreCase(ControllerType.COMMAND.name()) ) {
					// Configure the pipe - there should only be one per motor group.
					NodeList pipeElements = controllerElement.getElementsByTagName("pipe");
					if( pipeElements.getLength()>0) {
						Element pipeElement= (Element)(pipeElements.item(0));
						String pname = XMLUtility.attributeValue(pipeElement, "name");
						controllerTypes.put(name,type.toUpperCase());
						pipeNames.put(name,pname);
					}
				}
				else if( type!=null && !type.isBlank() &&
						type.equalsIgnoreCase(ControllerType.TERMINAL.name()) ) {
						// Configure the pipe - there should only be one per motor group.
						NodeList pipeElements = controllerElement.getElementsByTagName("pipe");
						if( pipeElements.getLength()>0) {
							Element pipeElement= (Element)(pipeElements.item(0));
							String pname = XMLUtility.attributeValue(pipeElement, "name");
							controllerTypes.put(name,type.toUpperCase());
							pipeNames.put(name,pname);
						}
					}
				
				index++;
			}
		}
	}
}
