/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package bert.command.model;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import bert.share.message.HandlerType;
import bert.share.model.AbstractRobotModel;
import bert.share.model.ConfigurationConstants;
import bert.share.xml.XMLUtility;

/**
 *  This is the base class for a collection of models that keep basic configuration
 *  information, all reading from the same files. The information 
 */
public class RobotCommandModel extends AbstractRobotModel   {
	private static final String CLSS = "RobotCommandModel";
	private static final Logger LOGGER = Logger.getLogger(CLSS);
	private int blueserverPort = 11046;
	private String deviceUUID  = "";
	private String deviceMAC   = "";

	public RobotCommandModel(Path configPath) {
		super(configPath);
	}
    
	
	/**
	 *  Analyze the document and populate the model. 
	 */
	public void populate() {
		analyzeControllers();
		analyzeProperties();
		analyzeMotors();
	}
	public int getBlueserverPort() { return this.blueserverPort; }
	public String getDeviceMAC() { return deviceMAC; }
	/**
	 * @return bluetooth device service UUID as a String
	 */
	public String getDeviceUUID() { return deviceUUID; }

    // ================================ Auxiliary Methods  ===============================
	/**
	 * Search the XML configuration for the command controller. It's the only one we care about.
	 * If not found, the controller will be null.
	 */
	@Override
	public void analyzeControllers() {
		if( this.document!=null ) {
			String controllerName = "UNASSIGNED";
			NodeList elements = document.getElementsByTagName("controller");
			int count = elements.getLength();
			int index = 0;
			while(index<count) {
				Element controllerElement= (Element)(elements.item(index));
				controllerName = XMLUtility.attributeValue(controllerElement, "name");
				String type = XMLUtility.attributeValue(controllerElement, "type");
				if( type!=null && !type.isEmpty() &&
					type.equalsIgnoreCase(HandlerType.COMMAND.name()) ) {
					// Configure the socket - there should only be one.
					NodeList socketElements = controllerElement.getElementsByTagName("socket");
					int nSocket = socketElements.getLength();
					if( nSocket>0) {
						handlerTypes.put(controllerName,type.toUpperCase());
						for( int iSocket=0;iSocket<nSocket;iSocket++) {
							Element socketElement= (Element)(socketElements.item(iSocket));
							String socketMAC = XMLUtility.attributeValue(socketElement, "mac");
							String socketName = XMLUtility.attributeValue(socketElement, "name");
							String portName = XMLUtility.attributeValue(socketElement, "port");
							String socketType = XMLUtility.attributeValue(socketElement, "type");
							String socketUUID = XMLUtility.attributeValue(socketElement, "uuid");
							if( socketType.equalsIgnoreCase("bluetooth")) {
								deviceUUID = socketUUID;
								deviceMAC  = socketMAC;
							}
							else {
								sockets.put(controllerName,Integer.parseInt(portName));
							}
						}
					}
					break;
				}
				
				index++;
			}
			properties.put(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, controllerName);
		}
	}
	/**
	 * Extend the default search for properties to convert the "blueserver" port to an int.
	 */
	@Override
	protected void analyzeProperties() {
		super.analyzeProperties();
		String port = properties.getProperty("blueserver");
		if( port!=null ) {
			try {
				this.blueserverPort = Integer.parseInt(properties.getProperty("blueserver"));
			}
			catch (NumberFormatException nfe) {
				LOGGER.warning(String.format("%s.analyzeProperties: Port for \"blueserver\" not a number (%s)",CLSS,nfe.getLocalizedMessage()));
			}
		}
		else {
			LOGGER.warning(String.format("%s.analyzeProperties: Port for \"blueserver\" missing in XML",CLSS));
		}
	}
}

