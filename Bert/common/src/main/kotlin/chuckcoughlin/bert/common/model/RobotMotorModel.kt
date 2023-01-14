/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.message.HandlerType
import chuckcoughlin.bert.common.util.XMLUtility
import jssc.SerialPort
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.nio.file.Path
import java.util.*
import java.util.logging.Logger

/**
 * The server-side model retains the configuration of all the request handlers
 * plus a hand-full of properties. It also reads the configuration file creating
 * for motor configuration objects which are the single place to obtain joint state.
 */
class RobotMotorModel(configPath: Path) : AbstractRobotModel(configPath) {
    private val jointsByController : MutableMap<String, List<Joint>>    // List of joints by controller name
    private val ports : MutableMap<String, SerialPort>           // Port objects by controller

    fun getJointsForController(controller: String): List<Joint>? {
        return jointsByController.get(controller)
    }

    fun getPortForController(controller: String): SerialPort? {
        return ports.get(controller)
    }

    // Analyze the document
    override fun populate() {
        analyzeProperties()
        analyzeControllers()
        analyzeSerialControllers()
        analyzeMotors()
    }
    // ================================ Auxiliary Methods  ===============================
    /**
     * Search the XML for the two command controllers (COMMAND and TERMINAL). Configure
     * messaging between them and this. This controller is "Dispatcher"
     */
    override fun analyzeControllers() {
        if (document != null) {
            val elements = document.getElementsByTagName("controller")
            val count = elements.length
            var index = 0
            while (index < count) {
                val controllerElement = elements.item(index) as Element
                val name: String = XMLUtility.attributeValue(controllerElement, "name")
                val type: String = XMLUtility.attributeValue(controllerElement, "type")
                if( type==HandlerType.COMMAND.name )  {
                    val socketElements = controllerElement.getElementsByTagName("socket")
                    if (socketElements.length > 0) {
                        handlerTypes.put(name,type)
                        val socketElement = socketElements.item(0) as Element
                        val portName: String = XMLUtility.attributeValue(socketElement, "port")
                        sockets.put(name,portName.toInt())
                    }
                }
                else if( type==HandlerType.TERMINAL.name ) {
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
    }

    /**
     * Search the XML for the SERIAL controllers. Create a map of joints by controller.
     */
    fun analyzeSerialControllers() {
        if (document != null) {
            val elements = document.getElementsByTagName("controller")
            val count = elements.length
            var index = 0
            while (index < count) {
                val controllerElement = elements.item(index) as Element
                val controller: String = XMLUtility.attributeValue(controllerElement, "name")
                val type: String = XMLUtility.attributeValue(controllerElement, "type")
                if( type == HandlerType.SERIAL.name ) {
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
    }

    companion object {
        private const val CLSS = "RobotMotorModel"
        private const val CONTROLLER_NAME = "Dispatcher"
        private val LOGGER = Logger.getLogger(CLSS)
    }
    init {
        jointsByController = mutableMapOf<String, List<Joint>>()
        ports              = mutableMapOf<String, SerialPort>()
    }
}