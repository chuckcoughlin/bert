/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 * Chat GPT interface is inspired by CJCrafter (Collin Barber)
 * @See https://github.com/CJCrafter/ChatGPT-Java-API/blob/master/README.md
 */
package chuckcoughlin.bert.speech.process

import chuckcoughlin.bert.common.message.JsonType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.model.Solver
import java.util.logging.Logger

/**
 * Handle tablet messages destined for ChatGPT
 */
object AIMessageHandler  {
    /**
     *  Send a list of link locations
     */
    fun getLinkLocations() : MessageBottle {
        val msg = MessageBottle(RequestType.JSON)
        msg.jtype = JsonType.LINK_LOCATIONS
        msg.text = Solver.linkLocationsToJSON()
        return msg
    }
    /**
     *  Send a list of limb names
     */
    fun getLimbNames() : MessageBottle {
        val msg = MessageBottle(RequestType.JSON)
        msg.jtype = JsonType.LIMB_NAMES
        msg.text = RobotModel.limbsToJSON()
        return msg
    }

    private val CLSS = "AIMessageHandler"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
    }
}