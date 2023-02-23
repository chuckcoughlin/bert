/**
 * Copyright 2022=2023. Charles Coughlin. All Rights Reserved.
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
 * without worrying about nulls.
 */
object RobotModel {
    private var document: Document? = null
    val coreControllers:  MutableList<String>  // Names of the internal controllers
    val motorControllerNames: MutableList<String>  // Names of the serial controllers
    val properties: Properties   // These are the generic properties
    val propertiesByController:  MutableMap<String, Properties>
    val jointsByController:      MutableMap<String,List<Joint>>
    val controllerTypes : MutableMap<String, ControllerType>   // Map of type for each con<troller by name
    val motors : MutableMap<Joint, MotorConfiguration> // Motor configuration by joint
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
     */
    fun populate() {
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
                properties[key.lowercase(Locale.getDefault())] = value
            }
            index++
        }
    }
    /**
     * Search the XML for named controllers. These have specific functions (i.e. types).
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
                try {
                    val ctype = ControllerType.valueOf(type)
                    controllerTypes[controllerName] = ctype
                    when(ctype) {
                        ControllerType.BITBUCKET -> {}
                        ControllerType.COMMAND -> {
                            val commandProperties = Properties()
                            val socket = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_SOCKET)
                            commandProperties[ConfigurationConstants.PROPERTY_SOCKET] = socket
                            val uuid = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_UUID)
                            commandProperties[ConfigurationConstants.PROPERTY_UUID] = uuid
                            propertiesByController[controllerName] = commandProperties
                        }
                        ControllerType.DISPATCHER -> {}
                        ControllerType.INTERNAL -> {}
                        // A motor controller controls motor devices associated with a single serial port
                        ControllerType.MOTOR -> {
                            val serialProperties = Properties()
                            val device = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_DEVICE)
                            serialProperties[ConfigurationConstants.PROPERTY_DEVICE] = device
                            val port = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_PORT)
                            serialProperties[ConfigurationConstants.PROPERTY_PORT] = port
                            propertiesByController[controllerName] = serialProperties
                            analyzeSerialController(controllerElement)
                        }
                        ControllerType.MOTORGROUP -> {}
                        ControllerType.SOCKET -> {
                            val socketProperties = Properties()
                            val socket = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_SOCKET)
                            socketProperties[ConfigurationConstants.PROPERTY_SOCKET] = socket
                            val hostName = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_HOSTNAME)
                            socketProperties[ConfigurationConstants.PROPERTY_HOSTNAME] = hostName
                            val port = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_PORT)
                            socketProperties[ConfigurationConstants.PROPERTY_PORT] = port
                            propertiesByController[controllerName] = socketProperties
                        }
                        ControllerType.TABLET -> {}
                        ControllerType.TERMINAL -> {
                            val terminalProperties = Properties()
                            val prompt = XMLUtility.attributeValue(controllerElement, ConfigurationConstants.PROPERTY_PROMPT)
                            if(!prompt.isEmpty())terminalProperties[ConfigurationConstants.PROPERTY_PROMPT] = prompt
                            propertiesByController[controllerName] = terminalProperties
                        }
                        ControllerType.UNDEFINED -> {}
                    }
                }
                catch(iae:IllegalArgumentException) {
                    LOGGER.warning(String.format("%s.analyzeControllers: %s is not a legal controller type ",
                        CLSS,type ))
                }
            }
            index++
        }
    }

    /**
     * Handle the extra XML for a SERIAL controllers. This includes creating a map of joints
     * associated with the controller. We've already the simple attributes. The simple
     * attributes contain enough information to define the SerialPort.
     */
    fun analyzeSerialController(controllerElement: Element) {
        val controller = XMLUtility.attributeValue(controllerElement, "name")
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
                //LOGGER.info(String.format("%s.analyzeSerialController: Added %s to %s",CLSS,jname,group));
            }
            catch (iae: IllegalArgumentException) {
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
        val controllers = document!!.getElementsByTagName("controller")
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

    /** ******************************** GETTERS ************************************
     *  The following are guaranteed to return non-null values
     */
    /* @return the name of the first controller in the list of controllers
     *         that matches the desired type. If none match, CONTROLLER_NOT_FOUND
     *          is returned.
     */
    fun getControllerForType(key: ControllerType) : String {
        var result = ConfigurationConstants.NO_CONTROLLER
        for( name in controllerTypes.keys ) {
            if( controllerTypes[name]!!.equals(key) ) {
                result = name
                break
            }
        }
        return result
    }
    fun getJointsForController(controller: String): List<Joint> {
        var joints = listOf<Joint>()
        val list = jointsByController[controller]
        if( list!=null ) joints = list
        return joints
    }
    /**
     * @return a named String property. If the requested property is not defined,
     *         return the string PROPERTY_NONE.
     */
    fun getPortForController(name:String): String {
        var port = ConfigurationConstants.NO_PORT
        val properties = propertiesByController[name]
        if( properties!=null ) {
            val pval = properties[ConfigurationConstants.PROPERTY_PORT]
            if( pval!=null && pval.toString().isNotBlank())
                port = pval.toString()
        }
        return port
    }
    /**
     * @return a named String property. If the requested property is not defined,
     *         return the string NO_VALUE.
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
     * @return a named String property. If the requested property is not defined,
     *         return the string PROPERTY_NONE.
     */
    fun getPropertyForController(name:String, key:String,defaultValue: String): String {
        var value = defaultValue
        val properties = propertiesByController[name]
        if( properties!=null ) {
            val pval = properties[key]
            if( pval!=null && !pval.toString().isBlank()) value = pval.toString()
        }
        return value
    }
    fun getPropertyForController(name:String,key:String): String {
        return getPropertyForController(name,key,ConfigurationConstants.NO_VALUE)
    }


    private val CLSS = "RobotMotorModel"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        properties = Properties()
        coreControllers           = mutableListOf<String>()
        motorControllerNames      = mutableListOf<String>()
        jointsByController        = mutableMapOf<String, List<Joint>>()
        propertiesByController    = mutableMapOf<String,Properties>()
        controllerTypes           = mutableMapOf<String,ControllerType>()
        motors                    = mutableMapOf<Joint, MotorConfiguration>()
    }
}