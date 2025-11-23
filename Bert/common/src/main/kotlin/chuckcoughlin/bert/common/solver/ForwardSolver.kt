/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.solver

import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.model.Appendage
import chuckcoughlin.bert.common.model.Chain
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.IMU
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.Link
import chuckcoughlin.bert.common.model.JointPosition
import chuckcoughlin.bert.common.model.Point3D
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.model.URDFModel
import chuckcoughlin.bert.common.util.TextUtility
import com.google.gson.GsonBuilder
import java.util.logging.Logger

/**
 * This class handles forward kinetics calculations.
 *
 * The URDFModel is the tree of links which describes the robot.
 * A single link object may belong to several chains.
 */
object ForwardSolver {
    val tree:JointTree

    /**
     * Return the orientation of a specified appendage in x,y,z coordinates in meters from the
     * robot origin in the pelvis. The named end effector or appendage is
     * last in the chain.
     */
    fun computeDirection(appendage: Appendage): DoubleArray {
        val subchain: List<Link> = Chain.partialChainToAppendage(appendage)
        val q = computeQuaternionFromChain(subchain)
        return q.direction()
    }

    /**
     * Return the orientation of a specified joint in x,y,z coordinates in meters from the
     * robot origin in the pelvis in the inertial reference frame. The named joint is
     * last in the chain.
     */
    fun computeDirection(joint: Joint): DoubleArray {
        val subchain: List<Link> = Chain.partialChainToJoint(joint)
        val q = computeQuaternionFromChain(subchain)
        return q.direction()
    }

    /**
     * Return a string for debugging use containing both position and direction
     */
    fun computePositionDescription(joint: Joint): String {
        val subchain: List<Link> = Chain.partialChainToJoint(joint)
        val q = computeQuaternionFromChain(subchain)
        return String.format("%s [%s]",q.positionToText(),q.directionToText())
    }
    /**
     * Return a string for debugging use containing both position and direction
     */
    fun computePositionDescription(appendage: Appendage): String {
        val subchain: List<Link> = Chain.partialChainToAppendage(appendage)
        val q = computeQuaternionFromChain(subchain)
        return String.format("%s [%s]",q.positionToText(),q.directionToText())
    }
    /**
     * Return the coordinated of a specified appendage in meters from the
     * robot origin in the pelvis in the inertial reference frame.
     * The named joint is last in the chain.
     */
    fun computePosition(appendage: Appendage): Point3D {
        val subchain: List<Link> = Chain.partialChainToAppendage(appendage)
        val q = computeQuaternionFromChain(subchain)
        return q.position()
    }

    /**
     * Return the coordinated of a specified joint in meters from the
     * robot origin in the pelvis in the inertial reference frame.
     * The named joint is last in the chain.
     */
    fun computePosition(joint: Joint): Point3D {
        val subchain: List<Link> = Chain.partialChainToJoint(joint)
        val q = computeQuaternionFromChain(subchain)
        return q.position()
    }

    /**
     * Update the link coordinates in a chain starting from the IMU, then multiply
     * quaternion matrices to get final position. The final position includes the
     * x,y,z position of the end effector with the orientation of the attached link.
     */
    private fun computeQuaternionFromChain(subchain: List<Link>):Quaternion {
        // Start with the IMU (Its orientation is updated externally)
        IMU.update()
        var q = IMU.quaternion   // Update for any rotation.
        if(DEBUG) LOGGER.info(String.format("%s.computeQuaternionFromChain: IMU = (%s|%s) ",
            CLSS,q.positionToText(),q.directionToText()))
        // Now continue up the chain
        for(link in subchain) {
            // if(DEBUG) LOGGER.info(String.format("%s.computeQuaternionFromChain: %s source %s = (%s) ",
            //    CLSS,link.name,if( link.sourcePin.joint==Joint.NONE) "IMU" else link.sourcePin.joint.name,q.position().toText()))
            link.update()  // In case motor has moved since last use
            //if(DEBUG) LOGGER.info(link.quaternion.dump())
            q = q.postMultiplyBy(link.quaternion)
            if(DEBUG) LOGGER.info(String.format("%s.computeQuaternionFromChain: %s end    %s = (%s|%s) ",
                CLSS,link.name,link.endPin.joint.name,q.positionToText(),q.directionToText()))
            //if(DEBUG) LOGGER.info(q.dump())
        }
        return q
    }


    /*
     * Populate a list of positions for all joints and extremities
     * from the current joint angles.
     */
    private fun fillPositions(tree:JointTree) {

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