/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.term.model

import bert.share.message.HandlerType
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.nio.file.Path

/**
 * Read the XML configuration file to extract information needed by
 * the Terminal application.
 */
class RobotTerminalModel(configPath: Path?) : AbstractRobotModel(configPath) {
    /**
     * Analyze the document and populate the model.
     */
    fun populate() {
        analyzeProperties()
        analyzeControllers()
    }
    // ================================ Auxiliary Methods  ===============================
    /**
     * Search the XML configuration for the terminal controller. It's the only one we care about.
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
                    type.equals(HandlerType.TERMINAL.name(), ignoreCase = true)
                ) {
                    // Configure the socket - there should only be one.
                    val socketElements = controllerElement.getElementsByTagName("socket")
                    if (socketElements.length > 0) {
                        handlerTypes.put(controllerName, type.uppercase(Locale.getDefault()))
                        val socketElement = socketElements.item(0) as Element
                        val portName: String = XMLUtility.attributeValue(socketElement, "port")
                        sockets.put(controllerName, portName.toInt())
                    }
                    break
                }
                index++
            }
            properties.put(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, controllerName)
        }
    }

    companion object {
        private const val CLSS = "RobotTerminalModel"
    }
}