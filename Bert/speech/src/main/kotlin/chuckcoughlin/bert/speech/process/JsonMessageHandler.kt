/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.process

import chuckcoughlin.bert.common.message.JsonType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.FaceDirection
import chuckcoughlin.bert.common.model.FacialDetails
import com.google.gson.Gson
import java.util.logging.Logger

/**
 * Handle a JSON message from the tablet. This is a
 * direct tablet-to-robot interaction. A JSON message
 * with no body is interpreted as a request for the
 * indicated data type.
 */
object JsonMessageHandler  {
    private val gson: Gson
    private val msg: MessageBottle

    /**
     * We have received a JsonString. Analyze it and create a response message.
     * Ultimately, the response text is bundled into a message to the tablet.
     *
     * For many JsonTypes, the incoming message is simply treated as a request
     * to fill in the indicated data type.
     *
     * @param response
     * @return true on success
     */
    fun handleJson(type:JsonType,json:String) :MessageBottle {
        var msg = MessageBottle(RequestType.NOTIFICATION)
        if( type == JsonType.FACIAL_DETAILS) {
            val fdd = gson.fromJson(json, FacialDetails::class.java)
            msg = FaceMessageHandler.handleDetails(fdd)
        }
        else if( type == JsonType.FACE_DIRECTION) {
            val fd = gson.fromJson(json, FaceDirection::class.java)
            msg = FaceMessageHandler.handleDirection(fd)
        }
        else if( type == JsonType.FACE_NAMES) {
            msg = FaceMessageHandler.getFaceNames()
        }
        else if( type == JsonType.MOVE_JOINTS) {
            msg = PositionMessageHandler.moveJoints(json)
        }
        else if( type == JsonType.LIMB_NAMES) {
            msg = PositionMessageHandler.getLimbNames()
        }
        else if( type == JsonType.JOINT_COORDINATES) {
            msg = PositionMessageHandler.getJointCoordinates()
        }
        else if( type == JsonType.MOTOR_PROPERTIES) {
            msg = PropertiesMessageHandler.getMotorProperties()
        }
        else {
            msg.error = String.format("data message type \"%s\" not handled",type.name)
        }
        return msg
    }



    private val CLSS = "JsonMessageHandler"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        gson = Gson()
        msg = MessageBottle(RequestType.NONE)
    }
}