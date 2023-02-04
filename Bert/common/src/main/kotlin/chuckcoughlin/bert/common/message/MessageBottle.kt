/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * MIT License
 */
package chuckcoughlin.bert.common.message

import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.model.Appendage
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointDynamicProperty
import chuckcoughlin.bert.common.model.Limb
import com.fasterxml.jackson.module.kotlin.*
import java.io.Serializable
import java.util.logging.Logger

/**
 * This class represents the contents of requests and responses that are sent
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
data class MessageBottle (var type:RequestType) : Serializable {
    private var jointValues : MutableList<JointPropertyValue>   // Property values for one or more motors
    var appendage: Appendage // Message applies to this appendage
    var command : CommandType
    var controller: String
    var error : String       // Error message if not blank
    var handler: ControllerType // The subsystem to handle this message
    var joint : Joint        // Request applies to this joint (if one)
    var limb: Limb           // Message applies to this limb
    var metric: MetricType
    var pose: String
    var property: JointDynamicProperty   // Subject of the original request
    var source: String       // Origin of message
    var text : String        // Pronounceable text of a response

    /**
     * Set the number of millisecs that the motion commanded by this
     * request is expected to last. Often this represents the required
     * time buffer between subsequent commands, especially those affecting
     * the same sub-chain.
     * @param period ~ msecs
     */
    var duration: Long = 0 // ~msecs
    var responderCount = 0 // Number of controllers/motors that have contributed

    /**
     * Increment the responder count by a specified value.
     * @param increment
     * @return the new count
     */
    fun incrementResponderCount(increment: Int): Int {
        responderCount = responderCount + increment
        return responderCount
    }

    /**
     * Increment the responder count by 1.
     * @return the new count
     */
    fun incrementResponderCount(): Int {
        responderCount = responderCount + 1
        return responderCount
    }

    fun addJointValue(j: Joint,prop: JointDynamicProperty, value: Number) {
        jointValues.add(JointPropertyValue(j,prop,value))
    }
    // Use this version where the request is a query for values
    fun addJointValue(j: Joint,prop: JointDynamicProperty) {
        jointValues.add(JointPropertyValue(j,prop,Double.NaN))
    }
    fun clearJointValues() {
        jointValues.clear()
    }

    fun getPropertyValueIterator() : MutableListIterator<JointPropertyValue> {
        return jointValues.listIterator()
    }

    private val CLSS = "MessageBottle"
    protected val LOGGER = Logger.getLogger(CLSS)

    /**
     * Set initiial values for all message parameters
     */
    init {
        appendage   = Appendage.NONE
        jointValues = mutableListOf<JointPropertyValue>()
        command     = CommandType.NONE
        controller  = BottleConstants.NO_CONTROLLER
        error   =  BottleConstants.NO_ERROR   // No error
        handler = ControllerType.UNDEFINED
        joint = Joint.NONE                  // Name of relavant joint
        limb  = Limb.NONE
        metric = MetricType.NAME
        pose   = BottleConstants.NO_POSE
        property = JointDynamicProperty.NONE
        source = BottleConstants.NO_SOURCE
        text  = ""   // Text is the printable response
    }
}