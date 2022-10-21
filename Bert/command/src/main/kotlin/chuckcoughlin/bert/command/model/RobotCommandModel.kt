/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.command.model

import chuckcoughlin.bert.common.message.HandlerType
import chuckcoughlin.bert.common.model.AbstractRobotModel
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.util.XMLUtility
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.nio.file.Path
import java.util.*
import java.util.logging.Logger

/**
 * This is the base class for a collection of models that keep basic configuration
 * information, all reading from the same files. The information
 */
class RobotCommandModel(configPath: Path?) : AbstractRobotModel(configPath) {
    var blueserverPort = 11046
        private set

    /**
     * @return bluetooth device service UUID as a String
     */
    var deviceUUID = ""
        private set
    var deviceMAC = ""
        private set

    /**
     * Analyze the document and populate the model.
     */
    fun populate() {
        analyzeControllers()
        analyzeProperties()
        analyzeMotors()
    }
    // ================================ Auxiliary Methods  ===============================
    /**
     * Search the XML configuration for the command controller. It's the only one we care about.
     * If not found, the controller will be null.
     */
    fun analyzeControllers() {
        if (this.document != null) {
            var controllerName = "UNASSIGNED"
            val elements: NodeList = document.getElementsByTagName("controller")
            val count = elements.length
            var index = 0
            while (index < count) {
                val controllerElement = elements.item(index) as Element
                controllerName = XMLUtility.attributeValue(controllerElement, "name")
                val type: String = XMLUtility.attributeValue(controllerElement, "type")
                if (type != null && !type.isEmpty() &&
                    type.equals(HandlerType.COMMAND.name(), ignoreCase = true)
                ) {
                    // Configure the socket - there should only be one.
                    val socketElements = controllerElement.getElementsByTagName("socket")
                    val nSocket = socketElements.length
                    if (nSocket > 0) {
                        handlerTypes.put(controllerName, type.uppercase(Locale.getDefault()))
                        for (iSocket in 0 until nSocket) {
                            val socketElement = socketElements.item(iSocket) as Element
                            val socketMAC: String = XMLUtility.attributeValue(socketElement, "mac")
                            val socketName: String = XMLUtility.attributeValue(socketElement, "name")
                            val portName: String = XMLUtility.attributeValue(socketElement, "port")
                            val socketType: String = XMLUtility.attributeValue(socketElement, "type")
                            val socketUUID: String = XMLUtility.attributeValue(socketElement, "uuid")
                            if (socketType.equals("bluetooth", ignoreCase = true)) {
                                deviceUUID = socketUUID
                                deviceMAC = socketMAC
                            } else {
                                sockets.put(controllerName, portName.toInt())
                            }
                        }
                    }
                    break
                }
                index++
            }
            properties.put(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, controllerName)
        }
    }

    /**
     * Extend the default search for properties to convert the "blueserver" port to an int.
     */
    protected fun analyzeProperties() {
        super.analyzeProperties()
        val port: String = properties.getProperty("blueserver")
        if (port != null) {
            try {
                blueserverPort = properties.getProperty("blueserver").toInt()
            } catch (nfe: NumberFormatException) {
                LOGGER.warning(
                    String.format(
                        "%s.analyzeProperties: Port for \"blueserver\" not a number (%s)",
                        CLSS,
                        nfe.localizedMessage
                    )
                )
            }
        } else {
            LOGGER.warning(String.format("%s.analyzeProperties: Port for \"blueserver\" missing in XML", CLSS))
        }
    }

    companion object {
        private const val CLSS = "RobotCommandModel"
        private val LOGGER = Logger.getLogger(CLSS)
    }
}