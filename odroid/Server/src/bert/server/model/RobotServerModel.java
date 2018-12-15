/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.server.model;

import java.nio.file.Path;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bert.share.bottle.BottleConstants;
import bert.share.common.RobotConstants;
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
	private int cadence;   
	private String name;
	
	public RobotServerModel(Path configPath) {
		super(configPath);
		this.name = RobotConstants.ROBOT_NAME;
		this.cadence = 1000;       // ~msecs
	}
   
	
	// Analyze the document
	public void populate() {
		analyzeProperties();
	}
	
	public int getCadence() { return this.cadence; }
	public void setCadence(int c) { this.cadence = c; }
	public String getName() { return this.name; }
	public void setName(String nam) { this.name = nam; }

    // ================================ Auxiliary Methods  ===============================
	/**
	 * Search the model for property elements. Set model attributes from them.
	 * @param index
	 * @param model
	 */
	private void analyzeProperties() {
		if( this.document!=null ) {
			NodeList elements = document.getElementsByTagName("property");
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
					else if(name.equalsIgnoreCase(BottleConstants.PROPERTY_NAME) ) {
						setName(value);
					}

					else if(name.equalsIgnoreCase(BottleConstants.PROPERTY_CADENCE) ) {
						try {
							setCadence(Integer.parseInt(value));
						}
						catch(NumberFormatException nfe) {
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
}
