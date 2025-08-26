/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.process

import chuckcoughlin.bert.common.message.JsonType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.Appendage
import chuckcoughlin.bert.common.model.LinkLocation
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.solver.ForwardSolver
import com.google.gson.Gson
import java.util.logging.Logger

/**
 * Handle tablet messages dealing with link locations.
 */
object LocationMessageHandler  {
    val gson: Gson
    /**
     *  Send a list of link locations
     */
    fun getLinkLocations() : MessageBottle {
        val msg = MessageBottle(RequestType.JSON)
        msg.jtype = JsonType.LINK_LOCATIONS
        msg.text = ForwardSolver.linkLocationsToJSON()
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

    fun moveEndEffector(json:String): MessageBottle {
        val msg = MessageBottle(RequestType.PLACE_APPENDAGE)
        val link = gson.fromJson(json, LinkLocation::class.java)
        msg.appendage = Appendage.fromString(link.appendage)
        msg.values[0] = link.end.x
        msg.values[1] = link.end.y
        msg.values[2] = link.end.z
        return msg
    }

    private val CLSS = "LocationMessageHandler"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        gson = Gson()
    }
}