/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.control.urdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bert.control.model.ChainElement;
import bert.share.motor.MotorConfiguration;
import bert.share.xml.XMLUtility;

/**
 *  Parse the URDF file and make its contents available.
 */
public abstract class URDFModel  {
	private static final String CLSS = "URDFModel";
	protected Document document;
	private final Map<String,LinkedList<ChainElement>> chains;
	private static final Logger LOGGER = Logger.getLogger(CLSS);

	
	public URDFModel() {
		this.chains = new HashMap<>();

	}
    


	/**
	 * @return a map of type names for each message handler used by this application.
	 */
	public Map<String,LinkedList<ChainElement>> getChains() { return this.chains; }

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
	 * Search the model for controller elements with joint sub-elements. The results form a list
	 * of MotorConfiguration objects.
	 */
	protected void analyzeChains() {
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
									if( value!=null && !value.isEmpty()) motor.setMinAngle(Double.parseDouble(value));
									value = XMLUtility.attributeValue(joint, "max");
									if( value!=null && !value.isEmpty()) motor.setMaxAngle(Double.parseDouble(value));
									value = XMLUtility.attributeValue(joint, "speed");
									if( value!=null && !value.isEmpty()) motor.setSpeed(Double.parseDouble(value));
									value = XMLUtility.attributeValue(joint, "torque");
									if( value!=null && !value.isEmpty()) motor.setTorque(Double.parseDouble(value));
									value = XMLUtility.attributeValue(joint, "orientation");
									if( value!=null && !value.isEmpty()) {
										motor.setIsDirect(value.equalsIgnoreCase("direct"));
									}
									//motors.put(motor.getName().name(),motor);
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


