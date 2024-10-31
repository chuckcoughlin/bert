/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.process

import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.CommandType
import chuckcoughlin.bert.common.message.JointPropertyValue
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
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
            if (type.equals(RequestType.NOTIFICATION)) {
                "Received an empty notification."
            }
            else if(type.equals(RequestType.COMMAND)) {
                randomAcknowledgement()
            }
            else {
                String.format("Received an empty %s message.",msg.type.name)
            }
        }
        // Message has text
        else {
            if(type.equals(RequestType.GET_MOTOR_PROPERTY)) {
                val property: JointDynamicProperty=msg.jointDynamicProperty
                val joint: Joint=msg.joint
                val iterator: MutableListIterator<JointPropertyValue> = msg.getJointValueIterator()
                if(iterator.hasNext()) {
                    val value: Double=iterator.next().value.toDouble()
                    String.format("The %s of my %s is %.02f", property.name, Joint.toText(joint), value)
                }
                else {
                    text
                }
            }
            else if(type.equals(RequestType.INITIALIZE_JOINTS)) {
                text
            }
            // If the message type is JSON, then the text of the message has been set to the gson string
            else if(type.equals(RequestType.JSON)) {
                String.format("%s %s",msg.jtype.name,msg.text)
            }
            else if(type.equals(RequestType.MAP_POSE)) {
                randomAcknowledgement()
            }
            else if(type.equals(RequestType.NOTIFICATION)) {
                text
            }
            // A limb has a state of being rigid or not
            else if (type.equals(RequestType.SET_LIMB_PROPERTY)) {
                val limb: Limb = msg.limb
                val iterator:MutableListIterator<JointPropertyValue> = msg.getJointValueIterator()
                if( iterator.hasNext() ) {
                    val jpv: JointPropertyValue = iterator.next()
                    val propertyName: String = jpv.property.name
                    if (propertyName.equals(JointDynamicProperty.STATE.name, ignoreCase = true)) {
                        val value: String = jpv.value.toString()
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
                else {
                    ""
                }
            }
            else if (type.equals(RequestType.SET_MOTOR_PROPERTY)) {
                randomAcknowledgement()
            }
            else if (type.equals(CommandType.SET_POSE)) {
                String.format("I am at %s", msg.arg.lowercase(Locale.getDefault()))
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