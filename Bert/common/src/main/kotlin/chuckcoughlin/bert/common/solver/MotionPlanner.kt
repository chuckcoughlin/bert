/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.solver

import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.*
import com.google.gson.GsonBuilder
import java.util.logging.Logger

/**
 * This class handles inverse kinematics calculations.
 */
object MotionPlanner {
    /**
     * Transform a PLACE_END_EFFECTOR request into SET_JOINT_POSITIONS message
     * ready for the MotorController. The message is transformed in place.
     */
    fun transformRequest(request:MessageBottle) : MessageBottle {
        request.type = RequestType.SET_JOINT_POSITIONS
        request.error = checkAppendage(request.appendage)
        if( request.error==BottleConstants.NO_ERROR ) {
            val joints = jointsForAppendage(request.appendage)
            var tree=ForwardSolver.getCurrentTree()
            val current=tree.getJointPositionByName(request.appendage.name)
            val goal=current.copy()
            val dir=Direction.fromString(request.text)
            val offset=request.values[0]
            when (dir) {
                Direction.RIGHT -> goal.pos.y=goal.pos.y - offset
                Direction.LEFT -> goal.pos.y=goal.pos.y + offset
                Direction.FRONT -> goal.pos.x=goal.pos.x - offset
                Direction.BACK -> goal.pos.x=goal.pos.x + offset
                Direction.UP -> goal.pos.z=goal.pos.z + offset
                Direction.DOWN -> goal.pos.z=goal.pos.z - offset
                Direction.UNKNOWN -> TODO()
            }
            val movements: List<JointPosition> = planMotion(request.appendage,joints,current, goal)
            val gson=GsonBuilder().create()
            val json=gson.toJson(movements)
            request.text=json
        }
        return request
    }
    /**
     * Verify that the appendage is one that we can move.
     */
    private fun checkAppendage(appendage:Appendage): String {
        val error = when (appendage) {
            Appendage.LEFT_EAR -> "moving the left ear is not supported"
            Appendage.LEFT_EYE -> "moving the left eye is not supported"
            Appendage.LEFT_HEEL -> "moving the left heel is not supported"
            Appendage.NOSE ->      "wiggling my nose is not supported"
            Appendage.RIGHT_EAR -> "moving the right ear is not supported"
            Appendage.RIGHT_EYE -> "moving the right eye is not supported"
            Appendage.RIGHT_HEEL ->"moving the right heel is not supported"
            Appendage.NONE ->      "moving an unknown end effector is not supported"
            else -> BottleConstants.NO_ERROR
        }
        return error
    }

    // Given an appendage, these are the joints to be set.
    private fun jointsForAppendage(appendage:Appendage) : List<Joint> {
        val list = mutableListOf<Joint>()
        when(appendage) {
            // Assume elbow and shoulder y are the same
            Appendage.LEFT_FINGER -> {
                list.add(Joint.LEFT_ELBOW_Y)
                list.add(Joint.LEFT_SHOULDER_X)
                list.add(Joint.LEFT_SHOULDER_Y)
                list.add(Joint.LEFT_SHOULDER_Z)
            }
            Appendage.RIGHT_FINGER -> {
                list.add(Joint.RIGHT_ELBOW_Y)
                list.add(Joint.RIGHT_SHOULDER_X)
                list.add(Joint.RIGHT_SHOULDER_Y)
                list.add(Joint.RIGHT_SHOULDER_Z)
            }
            Appendage.LEFT_TOE -> {
                list.add(Joint.LEFT_KNEE_Y)
                list.add(Joint.LEFT_HIP_X)
                list.add(Joint.LEFT_HIP_Y)
                list.add(Joint.LEFT_HIP_Z)
            }
            Appendage.RIGHT_TOE -> {
                list.add(Joint.RIGHT_KNEE_Y)
                list.add(Joint.RIGHT_HIP_X)
                list.add(Joint.RIGHT_HIP_Y)
                list.add(Joint.RIGHT_HIP_Z)
            }
            else -> {}
        }
        return list
    }

    private fun planMotion(appendage:Appendage,joints:List<Joint>,current: JointPosition,goal:JointPosition):List<JointPosition> {
        val movements = mutableListOf<JointPosition>()
        return movements
    }

    private const val CLSS = "MotionPlanner"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    /**
     * Constructor:
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
    }
}