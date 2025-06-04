/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.process

import chuckcoughlin.bert.common.message.JsonType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.model.Solver
import java.util.logging.Logger

/**
 * Handle tablet messages dealing with motor properties.
 */
object PropertiesMessageHandler  {

    /**
     *  Send a list of limb names
     */
    fun getMotorProperties() : MessageBottle {
        val msg = MessageBottle(RequestType.JSON)
        msg.jtype = JsonType.MOTOR_PROPERTIES
        msg.text = RobotModel.propertiesToJSON()
        return msg
    }


    private val CLSS = "PropertiesMessageHandler"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
    }
}