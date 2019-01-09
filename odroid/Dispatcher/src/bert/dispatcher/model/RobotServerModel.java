/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.dispatcher.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import bert.share.common.NamedPipePair;
import bert.share.model.AbstractRobotModel;
import bert.share.xml.XMLUtility;

/**
 *  The server-side model retains the configuration of all the request handlers
 *  plus a hand-full of properties. 
 */
public class RobotServerModel extends AbstractRobotModel  {
	private static final String CLSS = "RobotServerModel";
	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private static final System.Logger.Level level = System.Logger.Level.WARNING;
	private final List<NamedPipePair> pipes;
			
	public RobotServerModel(Path configPath) {
		super(configPath);
		this.pipes = new ArrayList<>();
	}
   
	
	// Analyze the document
	public void populate() {
		analyzeProperties();
		analyzePipes();
	}
	public List<NamedPipePair> getPipes() { return this.pipes; }

    // ================================ Auxiliary Methods  ===============================
	/**
	 * Search the model for the terminal controller. It's the only one we care about.
	 * If not found, the controller will be null;
	 * @param index
	 * @param model
	 */
	private void analyzePipes() {
		if( this.document!=null ) {
			NodeList elements = document.getElementsByTagName("controller");
			int count = elements.getLength();
			int index = 0;
			while(index<count) {
				Element controllerElement= (Element)(elements.item(index));
				NodeList pipeElements = controllerElement.getElementsByTagName("pipe");
				if( pipeElements.getLength()>0) {
					Element pipeElement= (Element)(pipeElements.item(0));
					NamedPipePair pipe = new NamedPipePair(false);
					String name = XMLUtility.attributeValue(pipeElement, "name");
					pipe.setName(name);
					pipes.add(pipe);
				}
				
				index++;
			}
		}
	}
}
