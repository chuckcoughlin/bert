/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.command

import chuckcoughlin.bert.common.message.JsonType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.FaceDirection
import chuckcoughlin.bert.common.model.FacialDetails
import chuckcoughlin.bert.common.model.RobotModel
import com.google.gson.Gson
import java.util.logging.Logger

/**
 * We have received a JSON message from the tablet. Handle it.
 * @param sock for socket connection
 */
object CommandJsonHandler  {
    private val gson: Gson
    private val msg: MessageBottle

    /**
     * We have received a JsonString. Analyze it and create a response message
     *
     * @param response
     * @return true on success
     */
    fun handleJson(type:JsonType,json:String) :MessageBottle {
        var msg = MessageBottle(RequestType.NOTIFICATION)
        if( type == JsonType.FACIAL_DETAILS) {
            val fdd = gson.fromJson(json, FacialDetails::class.java)
            msg = CommandFaceHandler.handleDetails(fdd)
        }
        else if( type == JsonType.FACE_DIRECTION) {
            val fd = gson.fromJson(json, FaceDirection::class.java)
            msg = CommandFaceHandler.handleDirection(fd)
        }
        else {
            msg.error = String.format("data message type \"%s\" not handled",type.name)
        }
        return msg
    }



    private val CLSS = "CommandJsonHandler"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_COMMAND)
        gson = Gson()
        msg = MessageBottle(RequestType.NONE)
    }
}