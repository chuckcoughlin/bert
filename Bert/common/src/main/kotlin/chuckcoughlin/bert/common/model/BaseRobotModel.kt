/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.util.XMLUtility
import chuckcoughlin.bert.share.controller.NamedSocket
import jssc.SerialPort
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.nio.file.Path
import java.util.*
import java.util.logging.Logger
import javax.xml.parsers.DocumentBuilderFactory

/**
 * This is the base class for a collection of models that keep basic configuration
 * information, all reading from the same configuration file.
 */
open class BaseRobotModel(configPath: Path) {
    protected val document: Document
    val coreControllers:  MutableList<String>  // Names of the internal controllers
    val motorControllers: MutableList<String>  // Names of the serial controllers
    val properties: Properties   // These are the generic properties
    val propertiesByController:   MutableMap<String,Properties>
    val controllerByPort:        MutableMap<SerialPort,String>
    val controllerBySocket:      MutableMap<NamedSocket,String>
    val controllerTypes : MutableMap<String, ControllerType>   // Map of type for each con<troller by name
    val motors : MutableMap<Joint, MotorConfiguration> // Motor configuration by joint

    /**
     * Analyze the XML configuration document in its entirety. This must be called before the model is accessed.
     */
    fun populate() {
        analyzeProperties()
        analyzeCoreControllers()
        analyzeSerialControllers()
        analyzeMotors()
    }



    /**
     * Expand the supplied path as the configuration XML file.
     * @return the configuration, an XML document.
     */
    fun analyzePath(filePath: Path): Document {
        val xmlFile = File(filePath.toUri())
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = InputSource(StringReader(xmlFile.readText()))
        val doc = dBuilder.parse(xmlInput)
        return doc
    }
    // ================================ Auxiliary Methods  ===============================
    /**
     * Search the configuration file for property elements. These values refer to the robot
     * as a whole. The results are saved in the properties member,
     * always lower case.
     */
     fun analyzeProperties() {
        val elements = document.getElementsByTagName("property")
        val count = elements.length
        var index = 0
        while (index < count) {
            val propertyNode = elements.item(index)
            val key = XMLUtility.attributeValue(propertyNode, "name")
            val value = propertyNode.textContent
            if (value != null && !value.isEmpty()) {
                properties[key.lowercase(Locale.getDefault())] = value
            }
            index++
        }
    }
    /**
     * Search the XML for named controllers. These have specific functions (i.e. types).
     */
     fun analyzeCoreControllers() {
        val elements = document.getElementsByTagName("controller")
        val count = elements.length
        var index = 0
        while (index < count) {
            val controllerElement = elements.item(index) as Element
            val name: String = XMLUtility.attributeValue(controllerElement, "name")
            val type: String = XMLUtility.attributeValue(controllerElement, "type").uppercase()
            if( type== ControllerType.COMMAND.name )  {
                val socketElements = controllerElement.getElementsByTagName("socket")
                if (socketElements.length > 0) {
                    handlerTypes.put(name,type)
                    val socketElement = socketElements.item(0) as Element
                    val portName: String = XMLUtility.attributeValue(socketElement, "port")
                    sockets.put(name,portName.toInt())
                }
            }
            else if( type== ControllerType.TERMINAL.name ) {
                terminalProperties = Properties()
                val socketElements: NodeList = controllerElement.getElementsByTagName("socket")
                if (socketElements.length > 0) {
                    handlerTypes[name] = type.uppercase(Locale.getDefault())
                    val socketElement = socketElements.item(0) as Element
                    val portName: String = XMLUtility.attributeValue(socketElement, "port")
                    sockets[name] = portName.toInt()
                }
            }
            index++
        }
        properties[ConfigurationConstants.PROPERTY_CONTROLLER_NAME] =
            CONTROLLER_NAME // Name not in XML configuration

    }

    /**
     * Search the XML for the SERIAL controllers. Create a map of joints by controller.
     */
    fun analyzeSerialControllers() {
        val elements = document.getElementsByTagName("controller")
        val count = elements.length
        var index = 0
        while (index < count) {
            val controllerElement = elements.item(index) as Element
            val controller: String = XMLUtility.attributeValue(controllerElement, "name")
            val type: String = XMLUtility.attributeValue(controllerElement, "type")
            if( type == ControllerType.SERIAL.name ) {
                // Configure the port - there should only be one per motor controller.
                val portElements = controllerElement.getElementsByTagName("port")
                if (portElements.length > 0) {
                    handlerTypes[controller] = type.uppercase(Locale.getDefault())
                    val portElement = portElements.item(0) as Element
                    val device: String = XMLUtility.attributeValue(portElement, "device")
                    val port = SerialPort(device)
                    ports[controller] = port
                }
                // Create a map of joints for the controller
                val jointElements = controllerElement.getElementsByTagName("joint")
                val jcount = jointElements.length
                var jindex = 0
                val joints: MutableList<Joint> = ArrayList()
                while (jindex < jcount) {
                    val jointElement = jointElements.item(jindex) as Element
                    val jname: String =
                        XMLUtility.attributeValue(jointElement, "name").uppercase(Locale.getDefault())
                    try {
                        val joint = Joint.valueOf(jname)
                        joints.add(joint)
                        //LOGGER.info(String.format("%s.analyzeControllers: Added %s to %s",CLSS,jname,group));
                    }
                    catch (iae: IllegalArgumentException) {
                        LOGGER.warning(String.format("%s.analyzeControllers: %s is not a legal joint name ",
                            CLSS,jname ))
                    }
                    jindex++
                }
                jointsByController[controller] = joints
            }
            index++
        }
    }
    /**
     * Search the XML description for controller elements with joint sub-elements. The results form a list
     * of MotorConfiguration objects.
     */
    protected fun analyzeMotors() {
        val controllers = document.getElementsByTagName("controller")
        val count = controllers.length
        var index = 0
        while (index < count) {
            val controllerNode = controllers.item(index)
            val type = XMLUtility.attributeValue(controllerNode, "type")
            if( type.equals("SERIAL", ignoreCase = true )) {
                val controller = XMLUtility.attributeValue(controllerNode, "name")
                var node = controllerNode.firstChild
                while (node != null) {
                    if (node.nodeType == Node.ELEMENT_NODE) {
                        val joint = node as Element
                        if (joint.tagName == "joint") {
                            val j = Joint.valueOf(XMLUtility.attributeValue(joint, "name"))
                            val id = XMLUtility.attributeValue(joint, "id").toInt()
                            val typ = DynamixelType.valueOf(type.uppercase(Locale.getDefault()))
                            var value = XMLUtility.attributeValue(joint, "orientation")
                            val isDirect = value.equals("direct", ignoreCase = true)

                            val motor = MotorConfiguration(j,typ,id,controller,isDirect)
                            motor.offset = XMLUtility.attributeValue(joint, "offset").toDouble()
                            // The following attributes are optional
                            value = XMLUtility.attributeValue(joint, "min")
                            if(!value.isEmpty() ) motor.minAngle = value.toDouble()
                            value = XMLUtility.attributeValue(joint, "max")
                            if( !value.isEmpty()) motor.maxAngle = value.toDouble()
                            value = XMLUtility.attributeValue(joint, "speed")
                            if( !value.isEmpty()) motor.maxSpeed = value.toDouble()
                            value = XMLUtility.attributeValue(joint, "torque")
                            if( !value.isEmpty()) motor.maxTorque = value.toDouble()
                            value = XMLUtility.attributeValue(joint, "limb")
                            if( !value.isEmpty()) {
                                try {
                                    val limb = Limb.valueOf(value.uppercase(Locale.getDefault()))
                                    motor.limb = limb
                                }
                                catch (iae: IllegalArgumentException) {
                                    LOGGER.warning( String.format(
                                        "%s.analyzeMotors: %s has unknown limb %s",
                                        CLSS,motor.joint.name, value ))
                                }
                            }
                            motors[motor.joint] = motor
                            LOGGER.fine(String.format("%s.analyzeMotors: Found %s", CLSS, motor.joint.name))
                        }
                    }
                    node = node.nextSibling
                }
            }
            index++
        }
    }

    private val CLSS = "AbstractRobotModel"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        document = analyzePath(configPath)
        properties = Properties()
        coreControllers           = mutableListOf<String>()
        motorControllers          = mutableListOf<String>()
        propertiesByController    = mutableMapOf<String,Properties>()
        controllerTypes           = mutableMapOf<String,ControllerType>()
        motors                    = mutableMapOf<Joint, MotorConfiguration>()
        populate()
    }
}