/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.process

import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointDynamicProperty
import chuckcoughlin.bert.common.model.Limb
import java.util.*
import java.util.logging.Logger

/**
 * This translator takes "MessageBottles" and generates text meant to be spoken.
 * These are usually responses to requests or error messages
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
        val type: RequestType=msg.type
        var text: String = ""
        if( !msg.error.equals(BottleConstants.NO_ERROR)) {
            text = msg.error
        }
        if( text.isBlank()) {
            text = msg.text
        }
        LOGGER.info(String.format("%s.messageToText:received %s = %s",CLSS,type.name,text))

        // Handle messages that have no response
        text = if (text.isBlank()) {
            if(type.equals(RequestType.NOTIFICATION)) {
                "Received an empty notification."
            }
            else if(type.equals(RequestType.COMMAND)) {
                randomAcknowledgement()
            }
            else if(type.equals(RequestType.EXECUTE_POSE)) {
                String.format("I am %s", msg.arg.lowercase(Locale.getDefault()))
            }
            // A limb has a state of being rigid or not. We look at the first joint in the list
            // to determine
            else if(type.equals(RequestType.SET_LIMB_PROPERTY)) {
                val limb: Limb=msg.limb
                val property=msg.jointDynamicProperty
                if(property.equals(JointDynamicProperty.STATE)) {
                    if(msg.value == ConfigurationConstants.OFF_VALUE) {
                        String.format("My %s is now flexible ", Limb.toText(limb))
                    }
                    else {
                        String.format("My %s is rigid ", Limb.toText(limb))
                    }
                }
                else {
                    text
                }
            }
            else {
                String.format("Received an empty %s.",msg.type.name)
            }
        }
        // Message has text
        else {
            if(type.equals(RequestType.GET_MOTOR_PROPERTY)) {
                val property: JointDynamicProperty=msg.jointDynamicProperty
                val joint: Joint=msg.joint
                String.format("The %s of my %s is %.02f", property.name, Joint.toText(joint), msg.value)

            }
            else if(type.equals(RequestType.INITIALIZE_JOINTS)) {
                randomAcknowledgement()
            }
            // If the message type is JSON, then the text of the message has been set to the gson string
            else if(type.equals(RequestType.JSON)) {
                String.format("%s %s",msg.jtype.name,msg.text)
            }
            else if(type.equals(RequestType.NOTIFICATION)) {
                text
            }
            else if (type.equals(RequestType.SET_MOTOR_PROPERTY)) {
                text
            }
            else {
                text
            }
        }
        LOGGER.info(String.format("%s.messageToText (%s): %s",CLSS,msg.type.name,text))
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

    private val CLSS = "MessageTranslator"
    private val LOGGER = Logger.getLogger(CLSS)
}