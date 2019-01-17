/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.motor.model;


import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bert.share.common.Port;
import bert.share.controller.ControllerType;
import bert.share.model.AbstractRobotModel;
import bert.share.motor.MotorConfiguration;
import bert.share.xml.XMLUtility;

/**
 *  The server-side model retains the configuration of all the request handlers
 *  plus a hand-full of properties. 
 */
public class RobotMotorModel extends AbstractRobotModel  {
	private static final String CLSS = "RobotServerModel";
	private static final System.Logger LOGGER = System.getLogger(CLSS);
	private final Map<String,String> groups;   // Motor names by group
	private final Map<String,List<String>> joints;   // List of joints by group
	
	private final Map<String,Port> ports;                  // Port objects by group
	
	public RobotMotorModel(Path configPath) {
		super(configPath);
		this.groups = new HashMap<>();
		this.joints = new HashMap<>();
		this.ports = new HashMap<>();
	}
   
	public List<String> getJointNamesForGroup(String group) { return this.joints.get(group); }
	public Port getPortForGroup(String group) { return this.ports.get(group); }
	
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
				if( type!=null && !type.isBlank() &&
						type.equalsIgnoreCase(ControllerType.SERIAL.name()) ) {
					// Configure the port - there should only be one per motor group.
					NodeList portElements = controllerElement.getElementsByTagName("port");
					if( portElements.getLength()>0) {
						controllerTypes.put(group,type.toUpperCase());
						Element portElement= (Element)(portElements.item(0));
						String pname = XMLUtility.attributeValue(portElement, "name");
						String device = XMLUtility.attributeValue(portElement, "device");
						Port port = new Port(pname,device);
						ports.put(group,port);
					}
					// Create a map of joints for the group
					NodeList jointElements = controllerElement.getElementsByTagName("joint");
					int jcount = jointElements.getLength();
					int jindex = 0;
					List<String> jointNames = new ArrayList<>();
					joints.put(group, jointNames);
					while(jindex<jcount) {
						Element jointElement= (Element)(jointElements.item(jindex));
						String jname = XMLUtility.attributeValue(jointElement, "name");
						jointNames.add(jname);
						jcount++;
					}
				}			
				index++;
			}
		}
	}
	
	
}
