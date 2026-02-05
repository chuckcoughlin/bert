/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.solver

import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.common.solver.ForwardSolver.tree
import com.google.gson.GsonBuilder
import java.util.logging.Logger

/**
 * This class handles forward kinetics calculations.
 *
 * The URDFModel is the tree of links which describes the robot.
 * A single joint position object may belong to several chains.
 */
object ForwardSolver {
    var tree: JointTree   // Represents the current actual position.

    /**
     * Return the orientation of the named joint or appendage in x,y,z coordinates
     * in meters from the robot origin in the pelvis in the inertial reference frame.
     * The named joint is last in the chain.
     * @param joint or appendage
     */
    fun computeDirection(joint: Joint): DoubleArray {
        val subchain: List<JointLink> = tree.createLinkChain(joint)
        val q = computeQuaternionFromChain(subchain)
        return q.direction()
    }

    /**
     * Return the coordinates of a specified joint in meters from the
     * robot origin in the pelvis in the inertial reference frame.
     * The named joint is last in the chain.
     */
    fun computePosition(joint: Joint): Point3D {
        val subchain: List<JointLink> = tree.createLinkChain(joint)
        val q = computeQuaternionFromChain(subchain)
        return q.position()
    }
    /**
     * Return a string for debugging use containing both position and direction
     * @param joint or appendage
     */
    fun computePositionDescription(joint: Joint): String {
        val subchain: List<JointLink> = tree.createLinkChain(joint)
        val q = computeQuaternionFromChain(subchain)
        if(DEBUG) LOGGER.info(String.format("%s.computePositionDescription: %s = (%s [%s]) ",
            CLSS,joint.name,q.positionToText(),q.directionToText()))
        return String.format("%s [%s]",q.positionToText(),q.directionToText())
    }

    fun initialize() {
        tree = URDFModel.createJointTree()
    }

    /**
     * Update the link coordinates in a chain starting from the IMU, then multiply
     * quaternion matrices to get final position. The final position includes the
     * x,y,z position of the end effector with the orientation of the attached link.
     */
    private fun computeQuaternionFromChain(subchain: List<JointLink>):Quaternion {
        var q = Quaternion.identity()
        var atOrigin = true
        val joint = Joint.NONE
        for(link in subchain) {
            //val jp = tree.getOrCreateJointPosition(link.basic.sourceJoint)
            val jp = tree.getOrCreateJointPosition(joint)
            if( atOrigin ) {
                atOrigin = false
                q = link.transform
                if(DEBUG) q.logdetails("origin")
            }
            else {
                if (DEBUG) LOGGER.info(link.transform.dump("link"))
                q = q.postMultiplyBy(link.transform)
                if (DEBUG) LOGGER.info(String.format("%s.computeQuaternionFromChain: %s end    %s = (%s|%s) ",
                    CLSS, joint.name, joint.name, q.positionToText(), q.directionToText()))
                    //CLSS, link.basic.sourceJoint.name, link.basic.endJoint.name, q.positionToText(), q.directionToText()))
                if (DEBUG) LOGGER.info(q.dump("product"))
            }
         }
        return q
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