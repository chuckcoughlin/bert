/**
 * Copyright 2022-2024 Charles Coughlin. All rights reserved.
 * MIT License
 */
package chuckcoughlin.bert.common.message

import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.model.*
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.*
import java.io.IOException
import java.io.Serializable
import java.util.logging.Logger

/**
 * This class is a container for requests and responses that are sent
 * across channels between parts of the robot. Semantics are entirely
 * determined by the message type.
 *
 * There is no intrinsic difference between requests and
 * responses. We leave it to context to determine which is which.
 * There may be multiple affected joints and multiple properties.
 * The collection of properties determines whether or not the
 * message makes practical sense.
 *
 * The message affects either a single or multiple joints, and may change
 * multiple dynamic properties at a time.
 *
 *  Sets bottle type. The rest are settable
*/
data class MessageBottle (var type:RequestType) : Cloneable,Serializable {
    var extremity: Extremity // Message applies to this extremity
    var command : CommandType
    var error : String       // Error message if not blank
    var handler: ControllerType // The subsystem to handle this message
    var id: Long             // Unique identifier (used for serial processing)
    var joint : Joint        // Request applies to this joint (if one)
    var jtype: JsonType      // Type when message is JSON
    var limb: Limb           // Message applies to this limb
    var metric: MetricType
    var arg: String          // Use whenever the command requires a text argument
    var jointDefinitionProperty: JointDefinitionProperty   // Possible subject of the original request
    var jointDynamicProperty: JointDynamicProperty         // Possible subject of the original request
    var source: ControllerType       // Origin of the request
    var text : String        // Pronounceable text of a response
    var value: Double
    var control : ExecutionControl  // Parameters dealing with execution of the message

    /**
     * Set the number of millisecs that the motion commanded by this
     * request is expected to last. Often this represents the required
     * time buffer between subsequent commands, especially those affecting
     * the same sub-chain.
     * @param period ~ msecs
     */
    var duration: Long = 0 // ~msecs

    override public fun clone(): MessageBottle {
        val copy = MessageBottle(type)
        copy.extremity   = extremity
        copy.command     = command
        copy.error       = error
        copy.handler     = handler
        copy.joint       = joint
        copy.jtype       = jtype
        copy.limb        = limb
        copy.metric      = metric
        copy.arg         = arg
        copy.jointDefinitionProperty = jointDefinitionProperty
        copy.jointDynamicProperty    = jointDynamicProperty
        copy.source      = source
        copy.text        = text
        copy.control     = control.clone()
        return copy
    }

    // =================================== JSON ======================================
    fun toJSON(): String {
        val mapper = jacksonObjectMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        var json = ""
        try {
            json = mapper.writeValueAsString(this)
            LOGGER.info(String.format("%s.toJSON: [%s]", CLSS, json))
        }
        catch (ex: Exception) {
            LOGGER.severe(String.format("%s.toJSON: Exception (%s)", CLSS, ex.localizedMessage))
        }
        return json
    }

    companion object {
        private const val serialVersionUID = 4356286171135500644L
        protected const val CLSS = "MessageBottle"
        protected val LOGGER = Logger.getLogger(CLSS)

        // =================================== JSON ======================================
        fun fromJSON(json: String): MessageBottle? {
            var bottle: MessageBottle? = null
            val mapper = jacksonObjectMapper()
            try {
                bottle = mapper.readValue(json)
            }
            catch (jpe: JsonParseException) {
                LOGGER.severe(String.format("%s.fromJSON: Parse exception (%s) from %s",
                        CLSS, jpe.getLocalizedMessage(), json
                    )
                )
            }
            catch (jme: JsonMappingException) {
                LOGGER.severe(String.format("%s.fromJSON: Mapping exception (%s) from %s",
                        CLSS, jme.getLocalizedMessage(), json
                    )
                )
            }
            catch (ioe: IOException) {
                LOGGER.severe(String.format("%s.fromJSON: IO exception (%s)", CLSS, ioe.getLocalizedMessage()))
            }
            return bottle
        }
    }

    /**
     * Set initiial values for all message parameters
     */
    init {
        extremity   = Extremity.NONE
        command     = CommandType.NONE
        error   =  BottleConstants.NO_ERROR   // No error
        handler = ControllerType.UNDEFINED
        id = 0
        joint = Joint.NONE                  // Name of relavant joint
        jtype = JsonType.UNDEFINED
        limb  = Limb.NONE
        metric = MetricType.NAME
        arg    = BottleConstants.NO_ARG
        jointDefinitionProperty = JointDefinitionProperty.NONE
        jointDynamicProperty = JointDynamicProperty.NONE
        source = ControllerType.UNDEFINED
        text  = ""   // Text is the printable response
        value = Double.NaN
        control = ExecutionControl(BottleConstants.NO_DELAY)
    }
}