/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.solver

import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.model.*
import com.google.gson.GsonBuilder
import java.util.logging.Logger

/**
 * This class handles forward kinetics calculations.
 *
 * The URDFModel is the tree of links which describes the robot.
 * A single joint position object may belong to several chains.
 */
object ForwardSolver {
    val tree:JointTree   // Represents the current actual position.

    /**
     * Return the orientation of the named joint or appendage in x,y,z coordinates
     * in meters from the robot origin in the pelvis in the inertial reference frame.
     * The named joint is last in the chain.
     * @param name of joint or appendage
     */
    fun computeDirection(name: String): DoubleArray {
        val subchain: List<JointLink> = Chain.partialChainToJoint(name)
        val q = computeQuaternionFromChain(subchain)
        return q.direction()
    }

    /**
     * Return a string for debugging use containing both position and direction
     * @param name of joint or appendage
     */
    fun computePositionDescription(name: String): String {
        val subchain: List<JointLink> = Chain.partialChainToJoint(name)
        val q = computeQuaternionFromChain(subchain)
        return String.format("%s [%s]",q.positionToText(),q.directionToText())
    }

    /**
     * Return the coordinates of a specified joint in meters from the
     * robot origin in the pelvis in the inertial reference frame.
     * The named joint is last in the chain.
     */
    fun computePosition(name: String): Point3D {
        val subchain: List<JointLink> = Chain.partialChainToJoint(name)
        val q = computeQuaternionFromChain(subchain)
        return q.position()
    }

    /**
     * @return a copy of the JointTree updated to the current motor angles.
     */
    fun getCurrentTree() : JointTree {
        val currentTree = tree.clone()
        RobotModel.refreshTree(currentTree)
        return currentTree
    }
    fun jointCoordinatesToJson() :String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val list = ForwardSolver.tree.listJointPositions()
        return gson.toJson(list)
    }
    /**
     * Update the link coordinates in a chain starting from the IMU, then multiply
     * quaternion matrices to get final position. The final position includes the
     * x,y,z position of the end effector with the orientation of the attached link.
     */
    private fun computeQuaternionFromChain(subchain: List<JointLink>):Quaternion {
        // Update each of the links in the chain for the current joint angles
        RobotModel.refreshChain(subchain)
        val origin = subchain.get(0)
        var q = origin.quaternion
        if(DEBUG) LOGGER.info(String.format("%s.computeQuaternionFromChain: IMU = (%s) ",
                              CLSS,origin.end.pos.toText()))
        // Now continue up the chain
        for(link in subchain) {
            //if(DEBUG) LOGGER.info(link.quaternion.dump())
            q = q.postMultiplyBy(link.quaternion)
            if(DEBUG) LOGGER.info(String.format("%s.computeQuaternionFromChain: %s end    %s = (%s|%s) ",
                CLSS,link.name,link.end.name,q.positionToText(),q.directionToText()))
            //if(DEBUG) LOGGER.info(q.dump())
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
        tree = URDFModel.createJointTree()
    }
}