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
     * Work back toward the root from the specified end effector. The chain
     * is ordered beginning from the root.
     * @param appendage
     * @return
     */
    fun partialChainToAppendage(appendage: Appendage): List<Link> {
        val partial: LinkedList<Link> = LinkedList<Link>()
        var link = URDFModel.linkForAppendage[appendage]
        if( link==null ) LOGGER.warning(String.format("%s.partialChainToAppendage: No link found for %s",CLSS,appendage.name))
        while (link != null) {
            partial.addFirst(link)
            if(DEBUG) LOGGER.info(String.format("%s.partialChainToAppendage: %s - inserting %s (%s)",CLSS,appendage.name,link.name,link.sourcePin.type))
            if( link.sourcePin.type==PinType.ORIGIN ) break
            val joint = link.sourcePin.joint
            link = URDFModel.linkForJoint[joint] // NONE implies the source
        }
        return partial
    }

    /**
     * Work back toward the root link beginning with the indicated joint.
     * The chain starts with the root.
     * @param joint, the source
     * @return
     */
    fun partialChainToJoint(joint: Joint): List<Link> {
        val partial: LinkedList<Link> = LinkedList<Link>()
        var link = URDFModel.linkForJoint[joint]
        while( link != null ) {
            partial.addFirst(link)
            if (DEBUG) LOGGER.info(String.format("%s.partialChainToJoint: %s - inserting %s (%s)",
                CLSS,joint.name,link.name,link.sourcePin.type))
            if (link.sourcePin.type.equals(PinType.ORIGIN)) break
            val j = link.sourcePin.joint
            link = URDFModel.linkForJoint[j]
            if (DEBUG && link != null) LOGGER.info(String.format("%s.partialChainToJoint: %s - next is %s",
                CLSS, j.name,link.name))
        }
        return partial
    }

    /**
     * The axes are the Euler angles in three dimensions between the robot and the reference frame.
     * @param a three-dimensional array of rotational offsets between the robot and reference frame.
     *        Components are: yaw, pitch and roll. Roll angle is ignored.
     *
     */
    fun setOrientation(a: DoubleArray) {
        IMU.axis= a
    }



    private val CLSS = "Chain"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    init {
        DEBUG= RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
    }
}