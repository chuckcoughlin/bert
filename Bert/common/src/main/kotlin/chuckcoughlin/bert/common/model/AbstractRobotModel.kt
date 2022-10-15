/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import bert.share.xml.XMLUtility
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.logging.Logger

/**
 * This is the base class for a collection of models that keep basic configuration
 * information, all reading from the same file. The information retained is specific
 * to the application.
 */
abstract class AbstractRobotModel(configPath: Path) {
    protected val document: Document?
    protected val properties: Properties

    /**
     * @return a map of type names for each message handler used by this application.
     */
    val handlerTypes // Message handler types by handler name
            : Map<String, String>

    /**
     * @return a map of socket attributes keyed by controller name.
     * The key list is sufficient to get the controller names.
     */
    val sockets // Socket port by handler name
            : Map<String, Int>
    protected val motors // Motor configuration by motor name
            : MutableMap<Joint?, MotorConfiguration>

    init {
        document = analyzePath(configPath)
        properties = Properties()
        handlerTypes = HashMap()
        motors = HashMap()
        sockets = HashMap()
    }

    /**
     * Each application needs to extract the controller(s) of interest from the
     * configuration file. Presumably this method will be called as part of the
     * populate() process.
     */
    abstract fun analyzeControllers()

    /**
     * Analyze the document. The information retained is dependent on the context
     * (client or server). This must be called before the model is accessed.
     */
    abstract fun populate()
    fun getProperty(key: String?, defaultValue: String?): String {
        return properties.getProperty(key, defaultValue)
    }

    /**
     * @return a map of motor configuration objects by motor name (upper case).
     */
    fun getMotors(): Map<Joint?, MotorConfiguration> {
        return motors
    }

    /**
     * Expand the supplied path as the configuration XML file.
     * @return the configuration, an XML document.
     */
    private fun analyzePath(filePath: Path): Document? {
        var contents: Document? = null
        try {
            val bytes = Files.readAllBytes(filePath)
            if (bytes != null) {
                contents = XMLUtility.documentFromBytes(bytes)
            }
        } catch (ioe: IOException) {
            LOGGER.severe(
                String.format(
                    "%s.getConfiguration: Failed to read file %s (%s)",
                    CLSS, filePath.toAbsolutePath().toString(), ioe.localizedMessage
                )
            )
        }
        return contents
    }
    // ================================ Auxiliary Methods  ===============================
    /**
     * Search the model for property elements. The results are saved in the properties member.
     * Call this if the model has any properties of interest.
     */
    protected fun analyzeProperties() {
        if (document != null) {
            val elements = document.getElementsByTagName("property")
            val count = elements.length
            var index = 0
            while (index < count) {
                val propertyNode = elements.item(index)
                val key = XMLUtility.attributeValue(propertyNode, "name")
                if (key == null) {
                    LOGGER.warning(String.format("%s.analyzeProperties: Missing name attribute in property", CLSS))
                } else {
                    val value = propertyNode.textContent
                    if (value != null && !value.isEmpty()) {
                        properties[key.lowercase(Locale.getDefault())] = value
                    }
                }
                index++
            }
        }
    }

    /**
     * Search the model for controller elements with joint sub-elements. The results form a list
     * of MotorConfiguration objects.
     */
    protected fun analyzeMotors() {
        if (document != null) {
            val controllers = document.getElementsByTagName("controller")
            val count = controllers.length
            var index = 0
            while (index < count) {
                val controllerNode = controllers.item(index)
                val type = XMLUtility.attributeValue(controllerNode, "type")
                if (type != null && type.equals("SERIAL", ignoreCase = true)) {
                    val controller = XMLUtility.attributeValue(controllerNode, "name")
                    if (controller != null) {
                        var node = controllerNode.firstChild
                        while (node != null) {
                            if (node.nodeType == Node.ELEMENT_NODE) {
                                val joint = node as Element
                                if (joint.tagName == "joint") {
                                    val motor = MotorConfiguration()
                                    motor.controller = controller
                                    var value = XMLUtility.attributeValue(joint, "name")
                                    if (value != null) {
                                        val name = Joint.valueOf(value.uppercase(Locale.getDefault()))
                                        motor.joint = name
                                    }
                                    value = XMLUtility.attributeValue(joint, "type")
                                    if (value != null) motor.setType(value)
                                    value = XMLUtility.attributeValue(joint, "id")
                                    if (value != null) motor.id = value.toInt()
                                    value = XMLUtility.attributeValue(joint, "offset")
                                    if (value != null) motor.offset = value.toDouble()
                                    value = XMLUtility.attributeValue(joint, "min")
                                    if (value != null && !value.isEmpty()) motor.minAngle = value.toDouble()
                                    value = XMLUtility.attributeValue(joint, "max")
                                    if (value != null && !value.isEmpty()) motor.maxAngle = value.toDouble()
                                    value = XMLUtility.attributeValue(joint, "speed")
                                    if (value != null && !value.isEmpty()) motor.maxSpeed = value.toDouble()
                                    value = XMLUtility.attributeValue(joint, "torque")
                                    if (value != null && !value.isEmpty()) motor.maxTorque = value.toDouble()
                                    value = XMLUtility.attributeValue(joint, "orientation")
                                    if (value != null && !value.isEmpty()) {
                                        motor.setIsDirect(value.equals("direct", ignoreCase = true))
                                    }
                                    value = XMLUtility.attributeValue(joint, "limb")
                                    if (value != null && !value.isEmpty()) {
                                        try {
                                            val limb = Limb.valueOf(value.uppercase(Locale.getDefault()))
                                            motor.limb = limb
                                        } catch (iae: IllegalArgumentException) {
                                            LOGGER.warning(
                                                String.format(
                                                    "%s.analyzeMotors: %s has unknown limb %s",
                                                    CLSS,
                                                    motor.joint.name,
                                                    value
                                                )
                                            )
                                        }
                                    }
                                    motors[motor.joint] = motor
                                    LOGGER.fine(String.format("%s.analyzeMotors: Found %s", CLSS, motor.joint.name))
                                }
                            }
                            node = node.nextSibling
                        }
                    } else {
                        LOGGER.warning(String.format("%s.analyzeProperties: Missing name attribute in property", CLSS))
                    }
                }
                index++
            }
        }
    }

    companion object {
        private const val CLSS = "AbstractRobotModel"
        private val LOGGER = Logger.getLogger(CLSS)
    }
}