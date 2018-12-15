/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.term.model;

import java.nio.file.Path;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bert.share.common.RobotConstants;
import bert.share.model.AbstractRobotModel;
import bert.share.xml.XMLUtility;

/**
 *  This is the base class for a collection of models that keep basic configuration
 *  information, all reading from the same files. The information 
 */
public class RobotTerminalModel extends AbstractRobotModel   {
	private static final String CLSS = "RobotClientModel";
	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private static final System.Logger.Level level = System.Logger.Level.WARNING;

	
	public RobotTerminalModel(Path configPath) {
		super(configPath);
	}
    
	
	/**
	 *  Analyze the document and populate the model. 
	 */
	public void populate() {
		analyzeDocument();
	}


    // ================================ Auxiliary Methods  ===============================
	/**
	 * Search the model for Property elements. Set model attributes from them.
	 * @param index
	 * @param model
	 */
	public void analyzeDocument() {
		if( this.document!=null ) {
			NodeList elements = document.getElementsByTagName("controller");
			int count = elements.getLength();
			int index = 0;
			while(index<count) {
				Node propertyNode = elements.item(index);
				String name = XMLUtility.attributeValue(propertyNode, "name");
				String value = propertyNode.getTextContent();
				if( value==null || value.isEmpty() ) {
					if( name==null ) {
						LOGGER.log(level,String.format("%s.getProperties: Missing name attribute in property",CLSS));
					}
				}
				else {
					LOGGER.log(level,String.format("%s.getProperties: Missing value attribute in %s property",CLSS,name));
				}
				
				index++;
			}
		}
	}
}