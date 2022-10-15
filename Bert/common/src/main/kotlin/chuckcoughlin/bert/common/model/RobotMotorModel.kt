/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import bert.share.message.HandlerType
import jssc.SerialPort
import org.w3c.dom.Element
import java.nio.file.Path
import java.util.logging.Logger

/**
 * The server-side model retains the configuration of all the request handlers
 * plus a hand-full of properties. It also reads the configuration file creating
 * for motor configuration objects which are the single place to obtain joint state.
 */
class RobotMotorModel(configPath: Path) : AbstractRobotModel(configPath) {
    private val jointsByController // List of joints by controller name
            : MutableMap<String, List<Joint>>
    private val ports // Port objects by controller
            : MutableMap<String, SerialPort>

    init {
        jointsByController = HashMap()
        ports = HashMap<String, SerialPort>()
    }

    fun getJointsForController(controller: String): List<Joint> {
        return jointsByController[controller]!!
    }

    fun getPortForController(controller: String): SerialPort? {
        return ports[controller]
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
                if (type != null && !type.isEmpty() &&
                    type.equals(HandlerType.COMMAND.name, ignoreCase = true)
                ) {
                    val socketElements = controllerElement.getElementsByTagName("socket")
                    if (socketElements.length > 0) {
                        handlerTypes[name] = type.uppercase(Locale.getDefault())
                        val socketElement = socketElements.item(0) as Element
                        val portName: String = XMLUtility.attributeValue(socketElement, "port")
                        sockets[name] = portName.toInt()
                    }
                } else if (type != null && !type.isEmpty() &&
                    type.equals(HandlerType.TERMINAL.name, ignoreCase = true)
                ) {
                    val socketElements = controllerElement.getElementsByTagName("socket")
                    if (socketElements.length > 0) {
                        handlerTypes[name] = type.uppercase(Locale.getDefault())
                        val socketElement = socketElements.item(0) as Element
                        val portName: String = XMLUtility.attributeValue(socketElement, "port")
                        sockets[name] = portName.toInt()
                    }
                }
                index++
            }
            properties[ConfigurationConstants.Companion.PROPERTY_CONTROLLER_NAME] =
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
                if (type != null && !type.isEmpty() &&
                    type.equals(HandlerType.SERIAL.name, ignoreCase = true)
                ) {
                    // Configure the port - there should only be one per motor controller.
                    val portElements = controllerElement.getElementsByTagName("port")
                    if (portElements.length > 0) {
                        handlerTypes[controller] = type.uppercase(Locale.getDefault())
                        val portElement = portElements.item(0) as Element
                        val pname: String = XMLUtility.attributeValue(portElement, "name")
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
                        } catch (iae: IllegalArgumentException) {
                            LOGGER.warning(
                                String.format(
                                    "%s.analyzeControllers: %s is not a legal joint name ",
                                    CLSS,
                                    jname
                                )
                            )
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
}