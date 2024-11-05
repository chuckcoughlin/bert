/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.command

import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.FaceDirection
import chuckcoughlin.bert.common.model.FacialDetails
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.sql.db.Database
import java.util.logging.Logger

/**
 * We have received a JSON message from the tablet. Handle it.
 * @param sock for socket connection
 */
object CommandFaceHandler  {
    private val msg: MessageBottle
    private var pending: FacialDetails?

    /**
     * The tablet has detected a face.
     * 1) If it matches a known face ,then send a greeting
     * 2) Otherwise save the details as pending and request
     *    an identifying name to go with it.
     *
     * @param response
     * @return a notification to the user.
     */
    fun handleDetails(details: FacialDetails) :MessageBottle {
        var faceId = Database.matchDetailsToFace(details)
        var msg = MessageBottle(RequestType.NOTIFICATION)

        return msg
    }
    /**
     * The tablet has detected a face.
     * Turn the robot head and eyes in the direction of the face.
     *
     * @param response
     * @return a notification to the user.
     */
    fun handleDirection(direction: FaceDirection) :MessageBottle {
        var msg = MessageBottle(RequestType.NOTIFICATION)

        return msg
    }

    /**
     * We are given the name that belongs to the pending details
     */
    fun handleFaceName(name:String) {

    }



    private val CLSS = "CommandFaceHandler"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_COMMAND)
        msg = MessageBottle(RequestType.NOTIFICATION)
        pending = null
    }
}