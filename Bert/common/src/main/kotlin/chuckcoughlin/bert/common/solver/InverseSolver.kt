/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.solver

import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.model.Appendage
import chuckcoughlin.bert.common.model.Chain
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.IMU
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.Link
import chuckcoughlin.bert.common.model.LinkLocation
import chuckcoughlin.bert.common.model.Point3D
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.model.URDFModel
import chuckcoughlin.bert.common.solver.ForwardSolver.model
import chuckcoughlin.bert.common.util.TextUtility
import com.google.gson.GsonBuilder
import java.awt.SystemColor.text
import java.util.logging.Logger

/**
 * This class handles inverse kinematics calculations.
 */
object InverseSolver {
    /**
     * Create a series of motor control messages from the given placement message.
     * Any errors are communicated via a single notification in the list,
     */
    fun placementCommands(placementMessage: MessageBottle): List<MessageBottle> {
        var error = BottleConstants.NO_ERROR
        val list = mutableListOf<MessageBottle>()
        when (placementMessage.appendage) {
            Appendage.LEFT_EAR -> error = "moving the left ear is not supported"
            Appendage.LEFT_EYE -> error = "moving the left eye is not supported"
            Appendage.LEFT_FINGER -> error = HandSolver.place(placementMessage,list)
            Appendage.LEFT_HEEL -> error = "moving the left heel is not supported"
            Appendage.LEFT_TOE -> error = "moving the left toe is not supported"
            Appendage.NOSE -> error = "wiggling my nose is not supported"
            Appendage.RIGHT_EAR -> error = "moving the right ear is not supported"
            Appendage.RIGHT_EYE -> error = "moving the right eye is not supported"
            Appendage.RIGHT_FINGER -> error = HandSolver.place(placementMessage,list)
            Appendage.RIGHT_HEEL -> error = "moving the right heel is not supported"
            Appendage.RIGHT_TOE -> error = "moving the right toe is not supported"
            Appendage.NONE -> error = "moving an unknown end effector is not supported"
        }

        placementMessage.error = error  // Set error, if any, on original request
        return list
    }

    private const val CLSS = "InverseSolver"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    /**
     * Constructor:
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
    }
}