/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.util.XMLUtility
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
abstract open class AbstractRobotModel(configPath: Path) {
    protected val document: Document?
    protected val properties: Properties
    protected val handlerTypes : MutableMap<String, String>   // Map of type names for each message handler used by this application.
    val sockets : MutableMap<String, Int>           // Socket port by handler name. The list keys are sufficient to get the controller names.
    protected val motors : MutableMap<Joint, MotorConfiguration> // Motor configuration by joint

    /**
     * Each application needs to extract the definitiona for controller(s) of interest
     * from the configuration file. Presumably this method will be called as part of the
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
     * Expand the supplied path as the configuration XML file.
     * @return the configuration, an XML document.
     */
     protected fun analyzePath(filePath: Path): Document? {
        var contents: Document? = null
        try {
            val bytes = Files.readAllBytes(filePath)
            if (bytes != null) {
                contents = XMLUtility.documentFromBytes(bytes)
            }
        }
        catch (ioe: IOException) {
            LOGGER.severe(String.format("%s.getConfiguration: Failed to read file %s (%s)",
                    CLSS, filePath.toAbsolutePath().toString(), ioe.localizedMessage) )
        }
        return contents
    }
    // ================================ Auxiliary Methods  ===============================
    /**
     * Search the model for property elements. The results are saved in the properties member.
     * Call this if the model has any properties of interest.
     */
      open fun analyzeProperties() {
        if (document != null) {
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
    }

    /**
     * Search the XML description for controller elements with joint sub-elements. The results form a list
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
    }

    private val CLSS = "AbstractRobotModel"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        document = analyzePath(configPath)
        properties = Properties()
        handlerTypes              = mutableMapOf<String,String>()
        motors                    = mutableMapOf<Joint, MotorConfiguration>()
        sockets                   = mutableMapOf<String,Int>()
    }
}