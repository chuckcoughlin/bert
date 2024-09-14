/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.command

import chuckcoughlin.bert.common.message.JsonType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.RobotModel
import java.util.logging.Logger

/**
 * We have received a JSON message from the tablet. Handle it.
 * @param sock for socket connection
 */
object CommandJsonHandler  {
    private val msg: MessageBottle

    /**
     * We have received a JsonString. Analyze it and create a textual
     * response to be returned to the tablet.
     *
     * @param response
     * @return true on success
     */
    fun handleJson(type:JsonType,text:String) :MessageBottle {
        var json = text

        return msg
    }



    private val CLSS = "CommandJsonHandler"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_COMMAND)
        msg = MessageBottle(RequestType.NONE)
    }
}