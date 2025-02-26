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
 * all relative to the root link (i.e. origin).
 *
 * The URDF file format doesn't define things in the most convenient
 * order and so is not strictly followed.
 *
 * Changes to the "inertial frame" as detected by the IMU
 * are all handled here.
 */
object Chain {
    var root: Link
    private var origin: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)
    private var axis: DoubleArray   = doubleArrayOf(0.0, 0.0, 0.0)

    /**
     * Work back toward the root from the specified extremity. The chain
     * is ordered beginning from the root.
     * @param extremity
     * @return
     */
    fun partialChainToExtremity(extremity: Extremity): List<Link> {
        val partial: LinkedList<Link> = LinkedList<Link>()
        var link = URDFModel.linkForExtremity[extremity]
        if( link==null ) LOGGER.warning(String.format("%s.partialChainToextremity: No link found for %s",CLSS,extremity.name))
        while (link != null) {
            partial.addFirst(link)
            if(DEBUG) LOGGER.info(String.format("%s.partialChainToExtremity: %s - inserting %s (%s)",CLSS,extremity.name,link.bone.name,link.sourcePin.type))
            if( link.sourcePin.type.equals(PinType.ORIGIN)) break
            val joint = link.sourcePin.joint
            link = URDFModel.linkForJoint[joint]
            if( link==null ) LOGGER.warning(String.format("%s.partialChainToextremity: No link found for joint %s",CLSS,joint))
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
        if( link==null ) LOGGER.warning(String.format("%s.partialChainToJoint: No link found for %s",CLSS,joint.name))
        while (link != null) {
            partial.addFirst(link)
            if(DEBUG) LOGGER.info(String.format("%s.partialChainToJoint: %s - inserting %s (%s)",CLSS,joint.name,link.bone.name,link.sourcePin.type))
            if( link.sourcePin.type.equals(PinType.ORIGIN)) break
            val joint = link.sourcePin.joint
            link = URDFModel.linkForJoint[joint]
            if(DEBUG && link!=null) LOGGER.info(String.format("%s.partialChainToJoint: %s - next is %s",CLSS,joint.name,link.bone.name))
            else if(link==null) LOGGER.warning(String.format("%s.partialChainToJoint: No link found for joint %s",CLSS,joint))
        }
        return partial
    }

    /**
     * The axes are the Euler angles in three dimensions between the robot and the reference frame.
     * @param a three dimensional array of rotational offsets between the robot and reference frame.
     */
    fun setAxes(a: DoubleArray) {
        Chain.axis= a
    }

    /**
     * The origin is the offset of the IMU with respect to the origin of the robot.
     * @param o three dimensional array of offsets to the origin of the chain
     */
    fun setOrigin(o: DoubleArray) {
        Chain.origin= o
    }


    private val CLSS = "Chain"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    init {
        DEBUG= RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        root = Link(Bone.NONE)    // Must be reset to be useful
    }
}