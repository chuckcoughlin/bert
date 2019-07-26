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
import bert.share.motor.Joint;
import bert.share.xml.XMLUtility;
import jssc.SerialPort;

/**
 *  The server-side model retains the configuration of all the request handlers
 *  plus a hand-full of properties. It is used to populate the database Motor
 *  State Table on startup. 
 */
public class RobotMotorModel extends AbstractRobotModel  {
	private static final String CLSS = "MotorManager";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private final Map<String,List<Joint>> jointsByController;   // List of joints by controller name
	
	private final Map<String,SerialPort> ports;                 // Port objects by controller
	
	public RobotMotorModel(Path configPath) {
		super(configPath);
		this.jointsByController = new HashMap<>();
		this.ports  = new HashMap<>();
	}
   
	public List<Joint> getJointsForController(String controller) { return this.jointsByController.get(controller); }
	public SerialPort getPortForController(String controller) { return this.ports.get(controller); }
	
	// Analyze the document
	public void populate() {
		analyzeProperties();
		analyzeControllers();
		analyzeMotors();
	}

    // ================================ Auxiliary Methods  ===============================
	/**
	 * Search the XML for the SERIAL controllers. Create a map of joints by controller. 
	 */
	@Override
	public void analyzeControllers() {
		if( this.document!=null ) {
			NodeList elements = document.getElementsByTagName("controller");
			int count = elements.getLength();
			int index = 0;
			while(index<count) {
				Element controllerElement= (Element)(elements.item(index));
				String controller = XMLUtility.attributeValue(controllerElement, "name");
				String type = XMLUtility.attributeValue(controllerElement, "type");
				if( type!=null && !type.isEmpty() &&
						type.equalsIgnoreCase(HandlerType.SERIAL.name()) ) {
					// Configure the port - there should only be one per motor controller.
					NodeList portElements = controllerElement.getElementsByTagName("port");
					if( portElements.getLength()>0) {
						handlerTypes.put(controller,type.toUpperCase());
						Element portElement= (Element)(portElements.item(0));
						String pname = XMLUtility.attributeValue(portElement, "name");
						String device = XMLUtility.attributeValue(portElement, "device");
						SerialPort port = new SerialPort(device);
						ports.put(controller,port);
					}
					// Create a map of joints for the controller
					NodeList jointElements = controllerElement.getElementsByTagName("joint");
					int jcount = jointElements.getLength();
					int jindex = 0;
					List<Joint> joints = new ArrayList<>();
					while(jindex<jcount) {
						Element jointElement= (Element)(jointElements.item(jindex));
						String jname = XMLUtility.attributeValue(jointElement, "name").toUpperCase();
						try {
							Joint joint = Joint.valueOf(jname);
							joints.add(joint);
							//LOGGER.info(String.format("%s.analyzeControllers: Added %s to %s",CLSS,jname,group));
						}
						catch(IllegalArgumentException iae) {
							LOGGER.warning(String.format("%s.analyzeControllers: %s is not a legal joint name ",CLSS,jname));
						}
						jindex++;
					}
					jointsByController.put(controller, joints);
				}			
				index++;
			}
		}
	}	
}
