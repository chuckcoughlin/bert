/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.process


import chuckcoughlin.bert.common.message.*
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointDynamicProperty
import chuckcoughlin.bert.common.model.Limb
import java.util.*
import java.util.logging.Logger
import kotlin.contracts.InvocationKind

/**
 * This translator takes "Request Bottles" and generates text meant to be spoken.
 */
class MessageTranslator {
    // Acknowledgments to a greeting
    private val greets = arrayOf(
        "hi",
        "yes?",
        "hello"
    )

    // Acknowledgements to a statement
    private val acks = arrayOf(
        "O K",
        "okay",
        "acknowledged",
        "so noted",
        "done",
        "complete",
        "yup"
    )

    /**
     * In many cases, text is set in the Dispatcher or MotorController. Use those
     * versions preferentially.
     * @param msg the response
     * @return pronounceable text
     */
    fun messageToText(msg: MessageBottle): String {
        var text: String = ""
        if( !msg.error.equals(BottleConstants.NO_ERROR)) {
            text = msg.error
        }
        if( text.isEmpty()) {
            text = msg.text
        }
        if (text.isEmpty()) {
            val type: RequestType = msg.type
            text = if (type.equals(RequestType.NOTIFICATION)) {
                    "Received an empty notification."
                }
                else if (type.equals(RequestType.NONE)) {
                    "Received empty message."
                }
                else if (type.equals(RequestType.COMMAND)) {
                    randomAcknowledgement()
                }
                else if (type.equals(RequestType.GET_METRIC)) {
                    String.format("The metric %s is unknown",
                            msg.metric.name.lowercase())
                }
                else if (type.equals(RequestType.GET_CONFIGURATION)) {
                    "Motor metrics have been written to log files"
                }
                else if (type.equals(RequestType.GET_MOTOR_PROPERTY)) {
                    val property: JointDynamicProperty = msg.property
                    val joint: Joint = msg.joint
                    val value: Double = msg.getJointValue()
                    String.format("The %s of my %s is %.02f", property.name, Joint.toText(joint), value)
                }
                else if (type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
                    val controllerName: String = msg.getProperty(PropertyType.CONTROLLER_NAME, "")
                    val propertyName: String = msg.getProperty(PropertyType.PROPERTY_NAME, "")
                    String.format("%s motor %s have been written to log files",
                        controllerName, propertyName.lowercase(Locale.getDefault()))
                }
                else if (type.equals(RequestType.MAP_POSE)) {
                    randomAcknowledgement()
                }
                else if (type.equals(RequestType.SET_LIMB_PROPERTY)) {
                    val limb: Limb = msg.limb
                    val propertyName: String =
                        msg.getProperty(PropertyType.PROPERTY_NAME, JointProperty.UNRECOGNIZED.name)
                    if (propertyName.equals(JointProperty.STATE.name, ignoreCase = true)) {
                        val value: String = msg.getProperty(propertyName, "")
                        if (value == "0") {
                            String.format("My %s is flexible ", Limb.toText(limb))
                        }
                        else {
                            String.format("My %s is rigid ", Limb.toText(limb))
                        }
                    }
                    else {
                        String.format("My %s is set ", Limb.toText(limb))
                    }
                }
                else if (type.equals(RequestType.SET_MOTOR_PROPERTY)) {
                    randomAcknowledgement()
                }
                else if (type.equals(RequestType.SET_POSE)) {
                    val propertyName: String = msg.getProperty(PropertyType.POSE_NAME, "")
                    String.format("I am %s", propertyName.lowercase(Locale.getDefault()))
                }
            else {
                val property: String = msg.getProperty(PropertyType.PROPERTY_NAME, "unknown")
                val value: String = msg.getProperty(property, "unknown")
                String.format("Its %s is %s", property.lowercase(Locale.getDefault()), value)
            }
        }

        if (text.isEmpty()) {
            text = String.format("I don't understand the response for %s",
                msg.type.name.lowercase().replace("_", " ")
            )
        }
        return text
    }
    // ============================================== Helper Methods ===================================
    /**
     * @return an affirmative response.
     */
    fun randomAcknowledgement(): String {
        val rand = Math.random()
        val index = (rand * acks.size).toInt()
        return acks[index]
    }

    fun randomGreetingResponse(): String {
        val rand = Math.random()
        val index = (rand * greets.size).toInt()
        return greets[index]
    }

    companion object {
        private const val CLSS = "MessageTranslator"
        private val LOGGER = Logger.getLogger(CLSS)
    }
}