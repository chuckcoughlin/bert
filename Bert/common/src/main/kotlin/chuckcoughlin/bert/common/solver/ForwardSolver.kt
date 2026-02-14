/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.solver

import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.model.*
import java.util.logging.Logger

/**
 * This class handles forward kinetics calculations.
 *
 * The URDFModel defines the tree of links which make up the robot
 * skeleton. A link has a source joint and an end joint. The end joint
 * is the unique key to the link. A single source joint may belong to several
 * links.
 */
object ForwardSolver {
    var tree: JointTree   // Represents the current actual position.

    /**
     * Return the orientation of the named joint or appendage in x,y,z coordinates
     * in meters from the robot origin in the pelvis in the inertial reference frame.
     * The named joint is last in the chain.
     * @param joint or appendage
     */
    fun directionForJoint(joint: Joint): DoubleArray {
        val q = computeQuaternionForJoint(joint)
        return q.direction()
    }

    /**
     * Return the coordinates of the specified joint in meters from the
     * robot origin in the pelvis in the inertial reference frame.
     * The named joint is last in the chain.
     */
    fun positionForJoint(joint: Joint): Point3D {
        val q = computeQuaternionForJoint(joint)
        return q.position()
    }
    /**
     * Return a string for debugging use containing both position and direction
     * @param joint or appendage
     */
    fun computePositionDescription(joint: Joint): String {
        val q = computeQuaternionForJoint(joint)
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
    private fun computeQuaternionForJoint(joint: Joint):Quaternion {
        val subchain: List<JointLink> = tree.createLinkChain(joint)
        updateJointAngles(subchain)
        var q = Quaternion.identity()
        for(link in subchain) {
            val jp1 = tree.getOrCreateJointPosition(link.sourceJoint)
            val jp2  = tree.getOrCreateJointPosition(link.endJoint)
            if(jp2.joint==Joint.IMU) {
                q = Quaternion.rotationQuaternion(jp2)
            }
            else {
                val mc = RobotModel.motorsByJoint[link.sourceJoint]!!
                jp1.setJointAngle(mc.angle)
                val q1 = Quaternion.rotationQuaternion(jp1)
                val q2 = Quaternion.translationQuaternion(link)
                q = q.postMultiplyBy(q1).postMultiplyBy(q2)
                q.populatePosition(jp2)
            }
            if (DEBUG) {
                LOGGER.info(String.format("%s.computeQuaternionForJoint: %s end    %s = (%s|%s) ",
                    CLSS, joint.name, joint.name, q.positionToText(), q.directionToText()))
                    //CLSS, link.basic.sourceJoint.name, link.basic.endJoint.name, q.positionToText(), q.directionToText()))
                LOGGER.info(q.dump("product"))
            }
         }
        return q
    }

    /**
     * Populate joints in the chain to their current angles.
     */
    private fun updateJointAngles(chain:List<JointLink>) {
        for (jlink in chain) {
            if( jlink.sourceJoint==Joint.IMU ) continue

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