/**
 * Copyright 2022=2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.util.XMLUtility
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.nio.file.Path
import java.util.*
import java.util.logging.Logger
import javax.xml.parsers.DocumentBuilderFactory

/**
 * This generic robot model adds convenience methods for extracting specifics
 * without worrying about nulls. The only type of controllers recognized are
 * the serial MOTOR controllers. Keep track of which joints are controlled
 * by each.
 */
object RobotModel {
    private var document: Document? = null
    val motorControllerNames:   MutableList<String>  // Names of the serial controllers
    val motorControllerDevices: MutableMap<String,String>
    val properties: Properties   // These are the generic properties
    val propertiesByController:  MutableMap<ControllerType, Properties>
    val jointsByController:      MutableMap<String,List<Joint>>
    val motorsByJoint:           MutableMap<Joint, MotorConfiguration> // Motor configuration by joint
    val motorsById:              MutableMap<Int, MotorConfiguration>   // Motor configuration by id

    var debug: String
    // These values are set by the main application based on command-line flags.
    // Add pseudo-setters to set the values as properties.
    var useBluetooth: Boolean = false
    var useSerial: Boolean = false
    var useTerminal: Boolean = false

    /**
     * Expand the supplied path as the configuration XML file.
     * Define the XML document.
     */
    fun startup(filePath: Path) {
        val xmlFile = File(filePath.toUri())
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = InputSource(StringReader(xmlFile.readText()))
        document = dBuilder.parse(xmlInput)
    }
    /**
     * Analyze the XML configuration document in its entirety. This must be called before the model is accessed.
     * The debug string is set externally
     */
    fun populate() {
        DEBUG = debug.contains(ConfigurationConstants.DEBUG_CONFIGURATION)
        analyzeProperties()
        analyzeControllers()
        analyzeMotors()
    }
    /** ******************************* ANALYZE XML **********************************/
    /**
     * Search the configuration file for property elements. These values refer to the robot
     * as a whole. The results are saved in the properties member,
     * always lower case to allow for a case-insensitive compare.
     */
    fun analyzeProperties() {
        val elements = document!!.getElementsByTagName("property")
        val count = elements.length
        var index = 0
        while (index < count) {
            val propertyNode = elements.item(index)
            val key = XMLUtility.attributeValue(propertyNode, "name")
            val value = propertyNode.textContent
            if (value != null && !value.isEmpty()) {
                properties[key.lowercase()] = value
            }
            index++
        }
    }
    /**
     * Search the XML for named controllers. These have specific functions (i.e. types)
     * to the Dispatcher.
     */
    fun analyzeControllers() {
        val elements = document!!.getElementsByTagName("controller")
        val count = elements.length
        var index = 0
        while (index < count) {
            val controllerElement = elements.item(index) as Element
            val controllerName = XMLUtility.attributeValue(controllerElement, "name")
            val type = XMLUtility.attributeValue(controllerElement, "type")
            if( !controllerName.isEmpty() ) {
                val ctype = ControllerType.fromString(type)
                when(ctype) {
                    ControllerType.BITBUCKET -> {}
                    ControllerType.COMMAND -> {     // The command controller connects to Bluetooth
                        val commandProperties = Properties()
                        val socket = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_SOCKET)
                        commandProperties[ConfigurationConstants.PROPERTY_SOCKET] = socket
                        val hostname = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_HOSTNAME)
                        commandProperties[ConfigurationConstants.PROPERTY_HOSTNAME] = hostname
                        val port = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_PORT)
                        commandProperties[ConfigurationConstants.PROPERTY_PORT] = port  // Integer
                        val uuid = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_UUID)
                        commandProperties[ConfigurationConstants.PROPERTY_UUID] = uuid
                        propertiesByController[ctype] = commandProperties
                    }
                    ControllerType.DISPATCHER -> {}
                    ControllerType.INTERNAL -> {}
                    // A motor controller controls motor devices associated with a single serial port
                    ControllerType.MOTOR -> {
                        val device = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_DEVICE)
                        motorControllerDevices[controllerName] = device
                        analyzeSerialController(controllerElement)
                    }
                    ControllerType.MOTORGROUP -> {}
                    ControllerType.SOCKET -> {     // A socket connection
                        val commandProperties = Properties()
                        val socket = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_SOCKET)
                        commandProperties[ConfigurationConstants.PROPERTY_SOCKET] = socket
                        val hostname = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_HOSTNAME)
                        commandProperties[ConfigurationConstants.PROPERTY_HOSTNAME] = hostname
                        val port = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_PORT)
                        commandProperties[ConfigurationConstants.PROPERTY_PORT] = port  // Integer
                        val uuid = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_UUID)
                        commandProperties[ConfigurationConstants.PROPERTY_UUID] = uuid
                        propertiesByController[ctype] = commandProperties
                    }
                    ControllerType.TABLET -> {}
                    ControllerType.TERMINAL -> {
                        val terminalProperties = Properties()
                        val prompt = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_PROMPT)
                        if(!prompt.isEmpty())terminalProperties[ConfigurationConstants.PROPERTY_PROMPT] = prompt
                        propertiesByController[ctype] = terminalProperties
                    }
                    ControllerType.UNDEFINED -> {
                        LOGGER.warning(String.format("%s.analyzeControllers: %s is not a legal controller type ",
                            CLSS,type ))
                    }
                }
            }
            index++
        }
    }

    /**
     * Handle the extra XML for a MOTOR controller. This includes creating a map of joints
     * associated with the controller. We already have the simple attributes. The simple
     * attributes contain enough information to define the SerialPort.
     */
    fun analyzeSerialController(controllerElement: Element) {
        val controller = XMLUtility.attributeValue(controllerElement, "name")
        LOGGER.info(String.format("%s.analyzeSerialController: %s",CLSS,controller))
        // Create a map of joints for the controller
        val jointElements = controllerElement.getElementsByTagName("joint")
        val jcount = jointElements.length
        var jindex = 0
        val joints: MutableList<Joint> = ArrayList()
        while (jindex < jcount) {
            val jointElement = jointElements.item(jindex) as Element
            val jname = XMLUtility.attributeValue(jointElement, "name")
            val joint = Joint.fromString(jname)  // Case insensitive
            if( joint!=Joint.NONE ) {
                joints.add(joint)
                if(DEBUG) LOGGER.info(String.format("%s.analyzeSerialController: %s added %s",CLSS,controller,jname));
            }
            else {
                LOGGER.warning(String.format("%s.analyzeSerialController: %s is not a legal joint name ",
                    CLSS,jname ))
            }
            jindex++
        }
        jointsByController[controller] = joints
    }
    /**
     * Search the XML description for controller elements with joint sub-elements. The results form a list
     * of MotorConfiguration objects.
     */
    fun analyzeMotors() {
        LOGGER.info(String.format("%s.analyzeMotors",CLSS))
        val controllers = document!!.getElementsByTagName("controller")
        val count = controllers.length
        var index = 0
        while (index < count) {
            val controllerNode = controllers.item(index)
            val cname = XMLUtility.attributeValue(controllerNode, "name")
            motorControllerNames.add(cname)
            val type = XMLUtility.attributeValue(controllerNode, "type")
            if( type.equals("MOTOR", ignoreCase = true )) {
                var node = controllerNode.firstChild
                while (node != null) {
                    if (node.nodeType == Node.ELEMENT_NODE) {
                        val joint = node as Element
                        if (joint.tagName == "joint") {
                            val j = Joint.fromString(XMLUtility.attributeValue(joint, "name"))
                            val id = XMLUtility.attributeValue(joint, "id").toInt()
                            val typ = DynamixelType.fromString(XMLUtility.attributeValue(joint, "type"))
                            var value = XMLUtility.attributeValue(joint, "orientation")
                            val isDirect = value.equals("direct", ignoreCase = true)

                            val motor = MotorConfiguration(j,typ,id,cname,isDirect)
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
                                    val limb = Limb.fromString(value)
                                    motor.limb = limb
                                }
                                catch (iae: IllegalArgumentException) {
                                    LOGGER.warning( String.format("%s.analyzeMotors: %s has unknown limb %s",
                                        CLSS,motor.joint.name, value ))
                                }
                            }
                            motorsByJoint[motor.joint] = motor
                            motorsById[motor.id] = motor
                            if(DEBUG) LOGGER.info(String.format("%s.analyzeMotors: Found %s", CLSS, motor.joint.name))
                        }
                    }
                    node = node.nextSibling
                }
            }
            index++
        }
    }

    /** ******************************** GETTERS ************************************
     *  The following are guaranteed to return non-null values
     */
    fun getJointsForMotorController(controller: String): List<Joint> {
        var joints = listOf<Joint>()
        val list = jointsByController[controller]
        if( list!=null ) joints = list
        return joints
    }
    /**
     * @return a named String property. If the requested property is not defined,
     *         return the string PROPERTY_NONE.
     */
    fun getDeviceForMotorController(name:String): String {
        var device =  motorControllerDevices[name]
        if( device==null ) {
            device = ConfigurationConstants.NO_DEVICE
        }
        return device
    }
    /**
     * @return a named String property of the robot as a whole. If the requested
     * property is not defined, return the string NO_VALUE.
     */
    fun getProperty(key: String, defaultValue: String): String {
        var value = defaultValue
        var pval = properties.getProperty(key)
        if( pval!=null && !pval.isBlank() ) {
            value = pval.toString()
        }
        return value
    }
    fun getProperty(key: String): String {
        return getProperty(key, ConfigurationConstants.NO_VALUE)
    }
    /**
     * @return a named String property that is special to a controller type.
     */
    fun getPropertyForController(ctype:ControllerType, key:String,defaultValue: String): String {
        var value = defaultValue
        val properties = propertiesByController[ctype]
        if( properties!=null ) {
            val pval = properties[key]
            if( pval!=null && !pval.toString().isBlank()) value = pval.toString()
        }
        return value
    }
    fun getPropertyForController(ctype:ControllerType,key:String): String {
        return getPropertyForController(ctype,key,ConfigurationConstants.NO_VALUE)
    }

    private val CLSS = "RobotModel"
    private var DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        DEBUG = false
        properties = Properties()
        motorControllerDevices    = mutableMapOf<String, String>()
        motorControllerNames      = mutableListOf<String>()
        jointsByController        = mutableMapOf<String, List<Joint>>()
        propertiesByController    = mutableMapOf<ControllerType,Properties>()
        motorsById                = mutableMapOf<Int, MotorConfiguration>()
        motorsByJoint             = mutableMapOf<Joint, MotorConfiguration>()
        debug = ""
    }
}