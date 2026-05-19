/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import com.google.gson.GsonBuilder
import java.util.*
import java.util.logging.Logger

/**
 * Retain a tree of linked joint positions. Each joint is
 * associated with a quaternion for computing 3D co-ordinates.
 *
 * Calculate    -> recompute joint positions in 3D space
 * Update       -> refresh the joint angle
 */
class JointTree() {
    val posmap: MutableMap<Joint, JointPosition>
    val linkmap: MutableMap<Joint, JointLink>  // Key = endJoint
    val linkSequence: MutableList<JointLink>

    /**
     * Update the position and orientation of a joint and all
     * prior joints in the chain from the IMU. We assume that the joint angles for
     * every joint from the specified joint to the root have been preset
     */
    fun computeJointPosition(joint:Joint) : JointPosition {
        val chain = createLinkChain(joint)
        var jp = JointPosition()
        if(chain.lastIndex>=0 ) {
            computeJointPositionsInChain(chain)
            val link = chain[chain.lastIndex]
            jp = getJointPosition(link.endJoint)
        }
        else {
            LOGGER.warning(String.format("%s.computeJointPosition: %s has no joints in chain",
                    CLSS, joint.name))
        }
        return jp
    }
    /**
     * Iterate over all end-effectors, creating link chains.
     * In doing so we include all joints in at least one chain.
     */
    fun computeJointPositions() {
        if (DEBUG) LOGGER.info(String.format("%s.updateJointPositions ...",CLSS))
        val joints = mutableListOf<Joint>()
        for (joint in posmap.keys) {
            joints.add(joint)
        }
        for(joint in joints) {
            if(Joint.isEndEffector(joint) ) {
                if (DEBUG) LOGGER.info(String.format("%s.updateJointPositions at %s",CLSS,joint))
                computeJointPosition(joint)
            }
        }
    }
    /**
     * Update the link coordinates and orientation in the supplied chain.  Multiply
     * quaternion matrices to get final position. The final position includes the
     * x,y,z position of the end effector with the orientation of the attached link.
     * This also updates the joint position corresponding to the end joint.
     */
    fun computeJointPositionsInChain(subchain: List<JointLink>) {
        // Start with any oriention of the IMU
        var root = getJointPosition(Joint.IMU)
        var q = Quaternion.rotationQuaternion(root)
        for(link in subchain) {
            val jp1 = getJointPosition(link.sourceJoint)
            val jp2 = getJointPosition(link.endJoint)
            // Rotate around the source, then translate to the end joint
            val q1 = Quaternion.rotationQuaternion(link)
            val q2 = Quaternion.translationQuaternion(link)
            q = q.postMultiplyBy(q1).postMultiplyBy(q2)
            q.updatePosition(jp2)
            if (DEBUG) {
                LOGGER.info(String.format("%s.computeJointPositionsInChain: %s -  %s = (%s|%s) ",
                        CLSS, jp1.joint.name, jp2.joint.name, q.positionToText(), q.directionToText()))
                //LOGGER.info(q.dump(jp2.joint.name))
            }
        }
    }
    fun createJointLink(source:Joint,joint:Joint) : JointLink {
        LOGGER.info(String.format("%s.createJointLink: %s to %s",CLSS,source.name,joint.name))
        val jlink = JointLink(source,joint)
        linkmap.put(joint, jlink)
        return jlink
    }
    /**
     * Work back toward the root link beginning with the indicated joint or
     * end effector. The chain starts with the link from root to next joint.
     *
     * @param j, name of the source joint.
     * @return a linked list of JointLinks
     */
     fun createLinkChain(j: Joint): List<JointLink> {
        val chain: LinkedList<JointLink> = LinkedList<JointLink>()
        var joint = j
        while(joint!=Joint.NONE) {
            val jlink= getJointLink(joint)
            chain.addFirst(jlink)
            if (DEBUG) LOGGER.info(String.format("%s.createLinkChain: %s - inserted %s",CLSS,j.name,joint.name))
            joint = jlink.sourceJoint
        }
        return chain
    }
    /**
     * Create a sequence of JointLinks where earlier joints
     * in the sequence are not dependent on later ones. This
     * cannot be called until after the tree is populated.
     */
    fun createLinkSequence() {
        var imu = getJointLink(Joint.IMU)
        var list = mutableListOf<JointLink>()
        list.add(imu)
        while(list.size>0) {
            addLinksToSequence(list)
            list = subsequentLinks(list)
        }
    }
    // The URDFModel carefully creates a link for each joint.
    // No links should be created here.
    fun getJointLink(end:Joint) : JointLink {
        var jlink = linkmap.get(end)
        if( jlink ==null ) {
            LOGGER.warning(String.format("%s.getJointLink: No link found for endJoint %s - created",CLSS,end.name))
            val jp = posmap.get(end)
            if(jp!=null) {
                jlink = createJointLink(Joint.IMU,end)
            }
            else {
                jlink = createJointLink(Joint.NONE,end)
            }
        }
        return jlink
    }

    /**
     * If the referenced position does not exist, create one.
     * The result parent must be updated if "NONE" is inappropriate.
     */
    fun getJointPosition(joint:Joint) : JointPosition {
        // LOGGER.info(String.format("%s.getJointPosition: %s",CLSS,name))
        var jp = posmap.get(joint)
        if(jp==null) {
            jp = JointPosition()
            jp.joint = joint
            posmap.put(joint,jp)
        }
        return jp
    }


    fun jointCoordinatesToJson() :String {
        val gson = GsonBuilder().create()
        return gson.toJson(listJointPositions())
    }

    /**
     * The tree is simply an unordered list of basic joint links.
     * NOTE: It appears that classes with LOGGERs cannot be serialized.
     */
    fun jointLinksToJson() :String {
        val gson = GsonBuilder().create()
        return gson.toJson(linkSequence)
    }
    fun listJointPositions() : List<JointPosition> {
        val list = mutableListOf<JointPosition>()
        for(jp in posmap.values) {
            if(jp.joint!=Joint.NONE) list.add(jp)
        }
        return list
    }

    /**
     * Populate all links in the tree to their
     * current physical angle. The angle refers to the angle of the source joint
     * to position the end joint.
     */
    fun setJointsToCurrent() {
        val root = getJointPosition(Joint.IMU)
        root.setOrientation(0.0,0.0,0.0)
        for (link in linkmap.values) {
            if( link.sourceJoint==Joint.IMU ) {
                // IMU angle is fixed at 180
                link.updateJointAngle(180.0)
                if(DEBUG) LOGGER.info(String.format("%s.setJointsToCurrent: %s -> %s = 180.0 (home=%2.0f)", CLSS,
                    link.sourceJoint.name, link.endJoint.name,link.home))
            }
            else if(link.sourceJoint!=Joint.NONE) {
                // Gives the right answer - wrong reason?
                val mc = RobotModel.motorsByJoint[link.endJoint]
                if (mc == null) {
                    LOGGER.info(String.format("%s.setJointsToCurrent: Missing link %s -> %s ...",CLSS,
                        link.sourceJoint.name, link.endJoint.name))
                }
                else {
                    link.updateJointAngle(mc.angle)
                    if(DEBUG) LOGGER.info(String.format("%s.setJointsToCurrent: %s -> %s = %2.0f (home=%2.0f)", CLSS,
                        link.sourceJoint.name, link.endJoint.name, mc.angle,link.home))
                }
            }
        }
    }
    /**
     * Populate all links in the tree to their
     * home angle. This is presumably the "straight" position.
     * Initialize the root position.
     */
    fun setJointsToHome() {
        val root = getJointPosition(Joint.IMU)
        root.setOrientation(0.0,0.0,0.0)
        for (jlink in linkmap.values) {
            jlink.updateJointAngle(jlink.home)
        }
    }


    fun setOrigin(jp:JointPosition) {
        jp.joint = Joint.IMU
        posmap.put(jp.joint,jp)
        LOGGER.info(String.format("%s.setOrigin: %s",
            CLSS,jp.joint.name))
    }

    /*
     * Populate all joint links for a specified limb to their
     * home angle. This is presuneably the "straight" position.
     */
    fun setLimbToHome(limb: Limb) {
        for (jlink in linkSequence) {
            val joint = jlink.sourceJoint
            val jlimb = RobotModel.limbsByJoint[joint]
            if( jlimb!=null && jlimb!=Limb.NONE && jlimb==limb ) {
                val jp = posmap.get(joint)
                if(jp!=null) jlink.updateJointAngle(jlink.home)
            }
        }
    }

    fun clone() : JointTree {
        val copy = JointTree()
        for(key in posmap.keys) {
            val jp = posmap.get(key)!!.copy()
            copy.posmap.put(key,jp)
        }
        for(key in linkmap.keys) {
            val jlink = linkmap.get(key)!!.clone()
            copy.linkmap.put(key,jlink)
        }
        copy.addLinksToSequence(linkSequence)
        return copy
    }

    //-------------------------------------------------------------------
    private fun addLinksToSequence(list:List<JointLink>) {
        for(jlink in list) {
            linkSequence.add(jlink)
        }
    }



    private fun subsequentLinks(links:List<JointLink>):MutableList<JointLink> {
        val list = mutableListOf<JointLink>()
        for(link in links) {
            if(Joint.isEndEffector(link.endJoint) ) continue
            for(jlink in linkmap.values ) {
                if(jlink.sourceJoint==link.endJoint) {
                    list.add(jlink)
                }
            }
        }
        return list
    }

    private val CLSS = "JointTree"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        posmap = mutableMapOf<Joint, JointPosition>()
        linkmap= mutableMapOf<Joint,JointLink>()
        linkSequence = mutableListOf<JointLink>()
    }
}
