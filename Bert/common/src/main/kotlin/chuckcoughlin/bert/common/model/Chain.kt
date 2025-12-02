/**
 * Copyright 2023-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import java.util.*
import java.util.logging.Logger

/**
 * The Chain is a tree of Links starting with the
 * "root" link. The position of links within the chain are
 * all relative to the IMU (i.e. origin).
 *
 * The URDF file format doesn't define things in the most convenient
 * order and so is not strictly followed.
 *
 * Changes to the "inertial frame" as detected by the IMU
 * are all handled here.
 */
object Chain {

    /**
     * Work back toward the root link beginning with the indicated joint or
     * end effector. The chain starts with the root.
     * @param joint, name of the source.
     * @return
     */
    fun partialChainToJoint(joint: String): List<JointLink> {
        val partial: LinkedList<JointLink> = LinkedList<JointLink>()
        val tree=URDFModel.createJointTree()
        var jp=tree.getJointPositionByName(joint)
        do {
            val jlink=tree.getJointLinkById(jp.id)
            partial.addFirst(jlink)
            // if (DEBUG) LOGGER.info(String.format("%s.partialChainToJoint: %s - inserting %s (%s)",
            //     CLSS,joint.name))
            jp=tree.getParent(jp)
        } while(jp.parent != ConfigurationConstants.NO_ID)

        return partial
    }

    private val CLSS = "Chain"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    init {
        DEBUG= RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
    }
}