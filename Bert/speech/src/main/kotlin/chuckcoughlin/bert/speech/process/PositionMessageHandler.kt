/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.process

import chuckcoughlin.bert.common.message.JsonType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.Appendage
import chuckcoughlin.bert.common.model.JointPosition
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.solver.ForwardSolver
import com.google.gson.Gson
import java.util.logging.Logger

/**
 * Handle tablet messages dealing with link locations.
 */
object PositionMessageHandler  {
    val gson: Gson
    /**
     *  Send a list of link locations
     */
    fun getJointCoordinates() : MessageBottle {
        val msg = MessageBottle(RequestType.JSON)
        msg.jtype = JsonType.JOINT_COORDINATES
        msg.text = ForwardSolver.jointCoordinatesToJson()
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

    fun moveJoints(json:String): MessageBottle {
        val msg = MessageBottle(RequestType.PLACE_END_EFFECTOR)
        val jp = gson.fromJson(json, JointPosition::class.java)
        msg.appendage = Appendage.fromString(jp.name)
        msg.values[0] = jp.pos.x
        msg.values[1] = jp.pos.y
        msg.values[2] = jp.pos.z
        return msg
    }

    private val CLSS = "LocationMessageHandler"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        gson = Gson()
    }
}