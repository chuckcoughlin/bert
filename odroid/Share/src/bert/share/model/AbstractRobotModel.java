/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.share.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bert.share.motor.MotorConfiguration;
import bert.share.xml.XMLUtility;

/**
 *  This is the base class for a collection of models that keep basic configuration
 *  information, all reading from the same file. The information retained is specific
 *  to the application.
 */
public abstract class AbstractRobotModel  {
	private static final String CLSS = "AbstractRobotModel";
	protected final Document document;
	protected final Properties properties;
	protected final Map<String,String> handlerTypes;  // Message handler types by handler name
	protected final Map<String,Integer> sockets;      // Socket port by handler name
	protected final Map<String,MotorConfiguration> motors; // Motor configuration by motor name
	private static final Logger LOGGER = Logger.getLogger(CLSS);

	
	public AbstractRobotModel(Path configPath) {
		this.document = analyzePath(configPath);
		this.properties = new Properties();
		this.handlerTypes = new HashMap<>();
		this.motors = new HashMap<>();
		this.sockets = new HashMap<>();
	}
    

	/**
	 *  Each application needs to extract the controller(s) of interest from the 
	 *  configuration file. Presumably this method will be called as part of the 
	 *  populate() process. 
	 */
	public abstract void analyzeControllers();
	
	/**
	 *  Analyze the document. The information retained is dependent on the context
	 *  (client or server). This must be called before the model is accessed.
	 */
	public abstract void populate();
	
	public String getProperty(String key,String defaultValue) {
		return this.properties.getProperty(key,defaultValue);
	}

	/**
	 * @return a map of type names for each message handler used by this application.
	 */
	public Map<String,String> getHandlerTypes() { return this.handlerTypes; }
	
	/**
	 * @return a map of motor configuration objects by motor name (upper case).
	 */
	public Map<String,MotorConfiguration> getMotors() { return this.motors; }
	/**
	 * @return a map of socket attributes keyed by controller name.
	 *         The key list is sufficient to get the controller names.
	 */
	public Map<String,Integer> getSockets() { return this.sockets; }
    /**
	 * Expand the supplied path as the configuration XML file.
	 * @return the configuration, an XML document.
	 */
	private Document analyzePath(Path filePath) {
		Document contents = null;
		try {
			byte[] bytes = Files.readAllBytes(filePath);
			if( bytes!=null ) {
				contents = XMLUtility.documentFromBytes(bytes);
			}
		}
		catch( IOException ioe) {
			LOGGER.severe(String.format("%s.getConfiguration: Failed to read file %s (%s)",
											CLSS,filePath.toAbsolutePath().toString(),ioe.getLocalizedMessage()));
		}
		return contents;
	}
	

    // ================================ Auxiliary Methods  ===============================

	
	/**
	 * Search the model for property elements. The results are saved in the properties member.
	 * Call this if the model has any properties of interest.
	 */
	protected void analyzeProperties() {
		if( this.document!=null ) {
			NodeList elements = document.getElementsByTagName("property");
			int count = elements.getLength();
			int index = 0;
			while(index<count) {
				Node propertyNode = elements.item(index);
				String key = XMLUtility.attributeValue(propertyNode, "name");
				if( key==null ) {
					LOGGER.warning(String.format("%s.analyzeProperties: Missing name attribute in property",CLSS));
				}
				else {
					String value = propertyNode.getTextContent();
					if( value!=null && !value.isBlank() ) {
						properties.put(key.toLowerCase(), value);
					}
				}
				index++;
			}
		}
	}
	
	/**
	 * Search the model for controller elements with joint sub-elements. The results form a list
	 * of MotorConfiguration objects.
	 */
	protected void analyzeMotors() {
		if( this.document!=null ) {
			NodeList controllers = document.getElementsByTagName("controller");
			int count = controllers.getLength();
			int index = 0;
			while(index<count) {
				Node controllerNode = controllers.item(index);
				String type = XMLUtility.attributeValue(controllerNode, "type");
				if( type!=null && type.equalsIgnoreCase("SERIAL")) {
					String controller = XMLUtility.attributeValue(controllerNode, "name");
					if( controller!=null ) {
						Node node = controllerNode.getFirstChild();
						while( node!=null ) {
							if( node.getNodeType()==Node.ELEMENT_NODE ) {
								Element joint = (Element)node;
								if( joint.getTagName().equals("joint") ) {
									MotorConfiguration motor = new MotorConfiguration();
									motor.setController(controller);
									String value = XMLUtility.attributeValue(joint, "name");
									if( value!=null) motor.setName(value);
									value = XMLUtility.attributeValue(joint, "type");
									if( value!=null) motor.setType(value);
									value = XMLUtility.attributeValue(joint, "id"); 
									if( value!=null) motor.setId(Integer.parseInt(value));
									value = XMLUtility.attributeValue(joint, "offset");
									if( value!=null) motor.setOffset(Double.parseDouble(value));
									value = XMLUtility.attributeValue(joint, "min");
									if( value!=null && !value.isBlank()) motor.setMinAngle(Double.parseDouble(value));
									value = XMLUtility.attributeValue(joint, "max");
									if( value!=null && !value.isBlank()) motor.setMaxAngle(Double.parseDouble(value));
									value = XMLUtility.attributeValue(joint, "speed");
									if( value!=null && !value.isBlank()) motor.setSpeed(Double.parseDouble(value));
									value = XMLUtility.attributeValue(joint, "torque");
									if( value!=null && !value.isBlank()) motor.setTorque(Double.parseDouble(value));
									value = XMLUtility.attributeValue(joint, "orientation");
									if( value!=null && !value.isBlank()) {
										motor.setIsDirect(value.equalsIgnoreCase("direct"));
									}
									motors.put(motor.getName().name(),motor);
									LOGGER.fine(String.format("%s.analyzeMotors: Found %s",CLSS,motor.getName().name()));
								}
							}
							
							node = node.getNextSibling();
						}
					}
					else {
						LOGGER.warning(String.format("%s.analyzeProperties: Missing name attribute in property",CLSS));
					}
				}
				
				index++;
			}
		}
	}
}

