/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.term.model;

import java.nio.file.Path;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import bert.share.common.PipeDirection;
import bert.share.common.PipeMode;
import bert.share.controller.ControllerType;
import bert.share.model.AbstractRobotModel;
import bert.share.model.NamedPipePair;
import bert.share.xml.XMLUtility;

/**
 *  This is the base class for a collection of models that keep basic configuration
 *  information, all reading from the same files. The information 
 */
public class RobotTerminalModel extends AbstractRobotModel   {
	private static final String CLSS = "RobotClientModel";
	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private static final System.Logger.Level level = System.Logger.Level.WARNING;
	private NamedPipePair pipe = null;

	
	public RobotTerminalModel(Path configPath) {
		super(configPath);
	}
   
	public NamedPipePair getPipe() { return this.pipe; }
	
	/**
	 *  Analyze the document and populate the model. 
	 */
	@Override
	public void populate() {
		analyzeProperties();
		analyzeControllers();
		analyzeMotors();
	}


    // ================================ Auxiliary Methods  ===============================
	/**
	 * Search the model for the terminal controller. It's the only one we care about.
	 * If not found, the controller will be null;
	 * @param index
	 * @param model
	 */
	private void analyzeControllers() {
		if( this.document!=null ) {
			NodeList elements = document.getElementsByTagName("controller");
			int count = elements.getLength();
			int index = 0;
			while(index<count) {
				Element controllerElement= (Element)(elements.item(index));
				String type = XMLUtility.attributeValue(controllerElement, "type");
				if( type!=null && !type.isBlank() &&
					type.equalsIgnoreCase(ControllerType.TERMINAL.name()) ) {
					// Configure the pipe - there should only be one.
					NodeList pipeElements = controllerElement.getElementsByTagName("pipe");
					if( pipeElements.getLength()>0) {
						Element pipeElement= (Element)(pipeElements.item(0));
						this.pipe = new NamedPipePair(false);
						String direction = XMLUtility.attributeValue(pipeElement, "direction");
						pipe.setDirection(PipeDirection.valueOf(direction.toUpperCase()));
						String mode = XMLUtility.attributeValue(pipeElement, "mode");
						pipe.setMode(PipeMode.valueOf(mode.toUpperCase()));
						String name = XMLUtility.attributeValue(pipeElement, "name");
						pipe.setName(name);
					}
					break;
				}
				
				index++;
			}
		}
	}
	
}