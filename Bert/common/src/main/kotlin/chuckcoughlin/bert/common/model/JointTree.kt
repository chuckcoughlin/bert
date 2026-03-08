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
 */
class JointTree() {
    val posmap: MutableMap<Joint, JointPosition>
    val linkmap: MutableMap<Joint, JointLink>  // Key = endJoint
    val linkSequence: MutableList<JointLink>

    /**
     * Iterate through all joints in the tree and compute orientation
     * and positions for a preset configuration. Assume each of the joint angles
     * has been set. The IMU joint is set to (0,0,0)
     */
    fun computeJointPositions() {
        val root = getOrCreateJointPosition(Joint.IMU)
        root.setOrientation(0.0,0.0,0.0)
        var q = Quaternion.identity()
        for(link in linkSequence) {
            val jp2 = posmap.get(link.endJoint)!!
            val q1 = Quaternion.rotationQuaternion(link)
            val q2 = Quaternion.translationQuaternion(link)
            q = q.postMultiplyBy(q1).postMultiplyBy(q2)
            jp2.updateFromQuaternion(q)
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
     * @param joint, name of the source.
     * @return a linked list of JointLinks
     */
    fun createLinkChain(j: Joint): List<JointLink> {
        val chain: LinkedList<JointLink> = LinkedList<JointLink>()
        var joint = j
        while(joint!=Joint.NONE) {
            val jlink= getOrCreateJointLink(joint)
            chain.addFirst(jlink)
            if (DEBUG) LOGGER.info(String.format("%s.createLinkChain: %s - inserted %s",CLSS,j.name,joint.name))
            joint = jlink.sourceJoint
        }

        return chain
    }

    // The URDFModel carefully creates links in order so that
    // no links are actually created here.
    fun getOrCreateJointLink(end:Joint) : JointLink {
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
    fun getOrCreateJointPosition(joint:Joint) : JointPosition {
        // LOGGER.info(String.format("%s.getJointPositionByName: %s",CLSS,name))
        var jp = posmap.get(joint)
        if(jp==null) {
            jp = JointPosition()
            jp.joint = joint
            posmap.put(joint,jp)
        }
        return jp
    }

    /**
     * @return the parent joint position. If the position
     *         does not exist, return the origin.
     */
    fun getParent(jp:JointPosition) : JointPosition {
        val jlink = getOrCreateJointLink(jp.joint)
        //return getOrCreateJointPosition(jlink.basic.sourceJoint)
        return getOrCreateJointPosition(jp.joint)
    }


    fun jointCoordinatesToJson() :String {
        val gson = GsonBuilder().create()
        return gson.toJson(listJointPositions())
    }

    fun listJointPositions() : List<JointPosition> {
        val list = mutableListOf<JointPosition>()
        for(jp in posmap.values) {
            list.add(jp)
        }
        return list
    }

    /**
     * Populate all joints in the tree to their
     * home angle. This is presuneably the "straight" position.
     * Initialize the root position.
     */
    fun setJointsToHome() {
        val root = getOrCreateJointPosition(Joint.IMU)
        root.setOrientation(0.0,0.0,0.0)
        for (jlink in linkmap.values) {
            jlink.setJointAngle(jlink.home)
        }
    }
    fun setOrigin(jp:JointPosition) {
        jp.joint = Joint.IMU
        posmap.put(jp.joint,jp)
        LOGGER.info(String.format("%s.setOrigin: %s",
            CLSS,jp.joint.name))
    }

    /**
     * The skeleton is simply an unordered list of basic joint links.
     * NOTE: It appears that classes with LOGGERs cannot be serialized.
     */
    fun skeletonToJson() :String {
        val gson = GsonBuilder().create()
        return gson.toJson(linkSequence)
    }

    /**
     * Update the position and orientation of a joint and all
     * connected joints. We assume that the joint angles for
     * every joint from the specified joint to the root have been preset
     */
    fun updateJointPosition(joint:Joint) : JointPosition {
        val chain = createLinkChain(joint)
        updateJointsInChain(chain)
        val link = chain[chain.lastIndex]
        return getOrCreateJointPosition(link.endJoint)!!
    }

    /**
     * Update the position and orientation of every joint position in the chain.
     * This assumes that the joint angles are preset appropriat4ely.
     * Use the linkSequence to update all joints at once
     */
    fun updatePositionsInLinkChain(chain:List<JointLink>) {
        updateJointsInChain(chain)
    }
//--------------------------
    /**
     * Update the link coordinates in a chain starting from the IMU, then multiply
     * quaternion matrices to get final position. The final position includes the
     * x,y,z position of the end effector with the orientation of the attached link.
     */
    private fun updateJointsInChain(subchain: List<JointLink>) {
        // Start with any oriention of the IMU
        var root = getOrCreateJointPosition(Joint.IMU)
        var q = Quaternion.rotationQuaternion(root)
        for(link in subchain) {
            val jp1 = getOrCreateJointPosition(link.sourceJoint)
            val jp2 = getOrCreateJointPosition(link.endJoint)
            // Rotate around the source, then translate to the end joint
            val q1 = Quaternion.rotationQuaternion(link)
            val q2 = Quaternion.translationQuaternion(link)
            q = q.postMultiplyBy(q1).postMultiplyBy(q2)
            jp2.updateFromQuaternion(q)
            if (DEBUG) {
                LOGGER.info(String.format("%s.updateJointsInChain: %s -  %s = (%s|%s) ",
                        CLSS, jp1.joint.name, jp2.joint.name, q.positionToText(), q.directionToText()))
                LOGGER.info(q.dump(jp2.joint.name))
            }
        }
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
                if(jp!=null) jlink.setJointAngle(jlink.home)
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
    /**
     * Create a sequence of JointLinks where earlier joints
     * in the sequence are not dependent on later ones. This
     * cannot be called until after the tree is populated.
     */
    fun createLinkSequence() {
        var imu = getOrCreateJointLink(Joint.IMU)
        var list = mutableListOf<JointLink>()
        list.add(imu)
        while(list.size>0) {
            addLinksToSequence(list)
            list = subsequentLinks(list)
        }
    }

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
