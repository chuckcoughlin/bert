/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.solver

import chuckcoughlin.bert.common.model.*
import java.util.logging.Logger

/**
 * This class handles forward kinetics calculations for the current
 * physical position of the joints.
 * .
 *
 * The URDFModel has defined the tree of links which make up the robot
 * skeleton. A link has a source joint and an end joint.
 */
object ForwardSolver {
    var tree: JointTree   // Represents the current actual position.

    fun directionForJoint(joint:Joint) : DoubleArray {
        val chain = tree.createLinkChain(joint)
        updateJointAngles(chain)
        tree.updateAllJointPositions()
        val jp = chain.get(chain.lastIndex)
        return jp.orientation
    }

    /**
     * Generate a Json description of the current joint links
     */
    fun skeletonToJson():String {
        tree.updateAllJointPositions()
        return tree.skeletonToJson()
    }
    fun positionForJoint(joint:Joint) : Point3D {
        val chain = tree.createLinkChain(joint)
        updateJointAngles(chain)
        tree.updateAllJointPositions()
        val jp = chain.get(chain.lastIndex)
        return Point3D(jp.coordinates[0],jp.coordinates[1],jp.coordinates[2])
    }

    /**
     * Generate a Json description of the current joint positions
     */
    fun jointCoordinatesToJson():String {
        tree.updateAllJointPositions()
        return tree.jointCoordinatesToJson()
    }

    /**
     * Update the position and orientation of every joint position in the chain.
     * Before making the calculation, we udpdate each joint position with
     * the current physical angle position.
     */
    fun updatePositionsInLinkChain(chain:List<JointLink>) {
        updateJointAngles(chain)
        tree.updatePositionsInLinkChain(chain)
    }

    fun initialize() {
        tree = URDFModel.createJointTree()
    }

    /**
     * Populate joints in the chain to their current angles.
     */
    private fun updateJointAngles(chain:List<JointLink>) {
        for (link in chain) {
            if( link.sourceJoint==Joint.IMU ) continue
            if( link.sourceJoint==Joint.NONE ) continue
            val mc = RobotModel.motorsByJoint[link.sourceJoint]!!
            link.setJointAngle(mc.angle)
        }
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