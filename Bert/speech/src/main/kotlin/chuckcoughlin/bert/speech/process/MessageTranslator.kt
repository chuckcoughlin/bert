/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.process

import bert.share.common.BottleConstants
import java.util.logging.Logger

/**
 * This translator takes "Request Bottles" and generate text meant to be spoken.
 *
 */
class MessageTranslator
/**
 * Constructor.
 */
{
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
    fun messageToText(msg: MessageBottle?): String? {
        var text: String? = null
        if (msg != null) {
            val error: String = msg.fetchError()
            if (error != null && !error.isEmpty()) {
                text = error
            }
            if (text == null || text.isEmpty()) {
                text = msg.getProperty(BottleConstants.TEXT, "")
            }
            if (text == null || text.isEmpty()) {
                val type: RequestType = msg.fetchRequestType()
                text = if (type.equals(RequestType.NOTIFICATION)) {
                    "Received an empty notification."
                } else if (type.equals(RequestType.NONE)) {
                    "Received empty message."
                } else if (type.equals(RequestType.COMMAND)) {
                    randomAcknowledgement()
                } else if (type.equals(RequestType.GET_METRIC)) {
                    val metric: MetricType = MetricType.valueOf(msg.getProperty(BottleConstants.METRIC_NAME, "NAME"))
                    java.lang.String.format("The metric %s is unknown", metric.name().toLowerCase())
                } else if (type.equals(RequestType.GET_CONFIGURATION)) {
                    "Motor metrics have been written to log files"
                } else if (type.equals(RequestType.GET_MOTOR_PROPERTY)) {
                    val propertyName: String = msg.getProperty(BottleConstants.PROPERTY_NAME, "")
                    val joint: Joint =
                        Joint.valueOf(msg.getProperty(BottleConstants.JOINT_NAME, Joint.UNKNOWN.name()).toUpperCase())
                    val value: String = msg.getProperty(propertyName, "")
                    java.lang.String.format("The %s of my %s is %s", propertyName, Joint.toText(joint), value)
                } else if (type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
                    val controllerName: String = msg.getProperty(BottleConstants.CONTROLLER_NAME, "")
                    val propertyName: String = msg.getProperty(BottleConstants.PROPERTY_NAME, "")
                    String.format(
                        "%s motor %s have been written to log files",
                        controllerName,
                        propertyName.lowercase(Locale.getDefault())
                    )
                } else if (type.equals(RequestType.MAP_POSE)) {
                    randomAcknowledgement()
                } else if (type.equals(RequestType.SET_LIMB_PROPERTY)) {
                    val limb: Limb =
                        Limb.valueOf(msg.getProperty(BottleConstants.LIMB_NAME, Limb.UNKNOWN.name()).toUpperCase())
                    val propertyName: String =
                        msg.getProperty(BottleConstants.PROPERTY_NAME, JointProperty.UNRECOGNIZED.name())
                    if (propertyName.equals(JointProperty.STATE.name(), ignoreCase = true)) {
                        val value: String = msg.getProperty(propertyName, "")
                        if (value == "0") {
                            java.lang.String.format("My %s is flexible ", Limb.toText(limb))
                        } else {
                            java.lang.String.format("My %s is rigid ", Limb.toText(limb))
                        }
                    } else {
                        java.lang.String.format("My %s is set ", Limb.toText(limb))
                    }
                } else if (type.equals(RequestType.SET_MOTOR_PROPERTY)) {
                    randomAcknowledgement()
                } else if (type.equals(RequestType.SET_POSE)) {
                    val propertyName: String = msg.getProperty(BottleConstants.POSE_NAME, "")
                    String.format("I am %s", propertyName.lowercase(Locale.getDefault()))
                } else {
                    val property: String = msg.getProperty(BottleConstants.PROPERTY_NAME, "unknown")
                    val value: String = msg.getProperty(property, "unknown")
                    String.format("Its %s is %s", property.lowercase(Locale.getDefault()), value)
                }
            }
        } else {
            text = "I received an empty message"
        }
        if (text == null || text.isEmpty()) {
            text = java.lang.String.format(
                "I don't understand the response for %s",
                msg.fetchRequestType().name().toLowerCase().replaceAll("_", " ")
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