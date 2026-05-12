/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.solver

import chuckcoughlin.bert.common.model.*
import java.util.logging.Logger

/**
 * This class handles forward kinetics calculations for the robot.
 * Methods are supplied that apply to the current physical position
 * of the motors. Otherwise, lower-level access allows the user to
 * configure the tree position separately.
 *
 * The URDFModel has defined the tree of links which make up the robot
 * skeleton. A link has a source joint and an end joint.
 */
object ForwardSolver {
    var tree: JointTree   // Represents the current actual position.

    fun currentDirectionForJoint(joint:Joint) : DoubleArray {
        tree.setJointsToCurrent()
        return directionForJoint(joint)
    }

    fun currentPositionForJoint(joint:Joint) : Point3D {
        tree.setJointsToCurrent()
        return positionForJoint(joint)
    }
    /**
     * Generate a Json description of the current joint positions
     */
    fun currentJointCoordinatesToJson():String {
        tree.setJointsToCurrent()
        tree.computeJointPositions()
        return jointCoordinatesToJson()
    }

    /**
     * Generate a Json description of the current joint links
     */
    fun currentJointLinksToJson():String {
        tree.setJointsToCurrent()
        tree.computeJointPositions()
        return jointLinksToJson()
    }

    fun directionForJoint(joint:Joint) : DoubleArray {
        val jp = tree.computeJointPosition(joint)
        return jp.orientation
    }


    fun positionForJoint(joint:Joint) : Point3D {
        val jp = tree.computeJointPosition(joint)
        return jp.pos
    }

    /**
     * Generate a Json description joint positions in the tree
     */
    fun jointCoordinatesToJson():String {
        return tree.jointCoordinatesToJson()
    }

    /**
     * Generate a Json description of the tree skeleton
     */
    fun jointLinksToJson():String {
        return tree.jointLinksToJson()
    }


    // Must be called after URDF is analyzed
    fun initialize() {
        tree = URDFModel.createJointTree()
    }


    private const val CLSS = "ForwardSolver"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    /**
     * Constructor:
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        tree = JointTree()
    }
}