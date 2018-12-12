/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package chuckcoughlin.bert.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import chuckcoughlin.bert.common.RobotConstants;
import chuckcoughlin.bert.xml.XMLUtility;

/**
 *  This class is the keeper of all knowledge of the robot configuration.
 */
public class RobotModel  {
	private final Document configuration;
	private final Map<Integer,PipeData> pipeMap = new HashMap<>();
	private static final String CLSS = "XMLUtility";
	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private static final System.Logger.Level level = System.Logger.Level.WARNING;
	private int cadence;   
	private String name;
	
	public RobotModel(Path configPath) {
		this.configuration = getConfiguration(configPath);
		this.name = RobotConstants.ROBOT_NAME;
		this.cadence = 1000;       // ~msecs
		initialize(this.configuration);
	}
    
    /**
	 * Expand the supplied path as the configuration XML file.
	 * @return the configuration, an XML document.
	 */
	public Document getConfiguration(Path filePath) {
		Document contents = null;
		try {
			byte[] bytes = Files.readAllBytes(filePath);
			if( bytes!=null ) {
				contents = XMLUtility.documentFromBytes(bytes);
			}
		}
		catch( IOException ioe) {
			LOGGER.log(level, String.format("%s.getConfiguration: Failed to read file %s (%s)",
											CLSS,filePath.toAbsolutePath().toString(),ioe.getLocalizedMessage()));
		}
		return contents;
	}
	
	// Analyze the document
	private void initialize(Document xml) {
		getProperties(xml);
	}
	
	public int getCadence() { return this.cadence; }
	public void setCadence(int c) { this.cadence = c; }
	public String getName() { return this.name; }
	public void setName(String nam) { this.name = nam; }

    // ================================ Auxiliary Methods  ===============================
	/**
	 * Search the model for Property elements. Set model attributes from them.
	 * @param index
	 * @param model
	 */
	public void getProperties(Document xml) {
		if( xml!=null ) {
			NodeList elements = xml.getElementsByTagName("Property");
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
					else if(name.equalsIgnoreCase(RobotConstants.PROPERTY_CADENCE) ) {
						try {
							setCadence(Integer.parseInt(value));
						}
						catch(NumberFormatException nfe) {
							LOGGER.log(level,String.format("%s.getProperties: Missing name attribute in property",CLSS));
						}

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

