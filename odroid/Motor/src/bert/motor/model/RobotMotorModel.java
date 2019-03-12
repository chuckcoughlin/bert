/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.model;


import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import bert.share.message.HandlerType;
import bert.share.model.AbstractRobotModel;
import bert.share.xml.XMLUtility;
import jssc.SerialPort;

/**
 *  The server-side model retains the configuration of all the request handlers
 *  plus a hand-full of properties. 
 */
public class RobotMotorModel extends AbstractRobotModel  {
	private static final String CLSS = "MotorManager";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private final Map<String,List<String>> joints;   // List of joints by group
	
	private final Map<String,SerialPort> ports;                  // Port objects by group
	
	public RobotMotorModel(Path configPath) {
		super(configPath);
		this.joints = new HashMap<>();
		this.ports  = new HashMap<>();
	}
   
	public List<String> getJointNamesForGroup(String group) { return this.joints.get(group); }
	public SerialPort getPortForGroup(String group) { return this.ports.get(group); }
	
	// Analyze the document
	public void populate() {
		analyzeProperties();
		analyzeControllers();
		analyzeMotors();
	}

    // ================================ Auxiliary Methods  ===============================
	/**
	 * Search the XML for the SERIAL controllers. Create a map of ports by controller. 
	 */
	@Override
	public void analyzeControllers() {
		if( this.document!=null ) {
			NodeList elements = document.getElementsByTagName("controller");
			int count = elements.getLength();
			int index = 0;
			while(index<count) {
				Element controllerElement= (Element)(elements.item(index));
				String group = XMLUtility.attributeValue(controllerElement, "name");
				String type = XMLUtility.attributeValue(controllerElement, "type");
				if( type!=null && !type.isEmpty() &&
						type.equalsIgnoreCase(HandlerType.SERIAL.name()) ) {
					// Configure the port - there should only be one per motor group.
					NodeList portElements = controllerElement.getElementsByTagName("port");
					if( portElements.getLength()>0) {
						handlerTypes.put(group,type.toUpperCase());
						Element portElement= (Element)(portElements.item(0));
						String pname = XMLUtility.attributeValue(portElement, "name");
						String device = XMLUtility.attributeValue(portElement, "device");
						SerialPort port = new SerialPort(device);
						ports.put(group,port);
					}
					// Create a map of joints for the group
					NodeList jointElements = controllerElement.getElementsByTagName("joint");
					int jcount = jointElements.getLength();
					int jindex = 0;
					List<String> jointNames = new ArrayList<>();
					while(jindex<jcount) {
						Element jointElement= (Element)(jointElements.item(jindex));
						String jname = XMLUtility.attributeValue(jointElement, "name").toUpperCase();
						jointNames.add(jname);
						LOGGER.fine(String.format("%s.analyzeControllers: Added %s to %s",CLSS,jname,group));
						jindex++;
					}
					joints.put(group, jointNames);
				}			
				index++;
			}
		}
	}	
}
