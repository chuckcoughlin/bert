/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.solver.ForwardSolver
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
     * has been set. The IMU joint is left at (0,0)
     */
    fun computeJointPositions() {
        var q = Quaternion.identity()
        for(link in linkSequence) {
            val jp1 = posmap.get(link.sourceJoint)!!
            val jp2 = posmap.get(link.endJoint)!!
            if(link.endJoint==Joint.IMU) {
                q = Quaternion.rotationQuaternion(jp2)
            }
            else {
                val q1 = Quaternion.rotationQuaternion(jp1)
                val q2 = Quaternion.translationQuaternion(link)
                q = q.postMultiplyBy(q1).postMultiplyBy(q2)
                q.populatePosition(jp2)
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
     * Create a new joint position. Add it to the tree.
     */
    fun createJointPosition(joint:Joint) : JointPosition {
        val jp = JointPosition()
        jp.joint = joint
        posmap.put(joint,jp)
        return jp
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
        do {
            val jlink= getOrCreateJointLink(joint)
            chain.addFirst(jlink)
            // if (DEBUG) LOGGER.info(String.format("%s.createLinkChain: %s - inserting %s (%s)",CLSS,joint.name))
            joint = jlink.sourceJoint
        } while(joint!=Joint.NONE)

        return chain
    }

    fun getOrCreateJointLink(end:Joint) : JointLink {
        //LOGGER.info(String.format("%s.getJointLink: %s",CLSS,end.name))
        var jlink = linkmap.get(end)
        if( jlink ==null ) {
            LOGGER.warning(String.format("%s.getJointLink: No link found for endJoint %s - created",CLSS,end.name))
            val jp = posmap.get(end)
            if(jp!=null) {
                jlink = JointLink(Joint.IMU,end)
            }
            else {
                jlink = JointLink(Joint.NONE,end)
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
            jp = createJointPosition(joint)
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

    /**
     * The skeleton is simply an unordered list of basic joint links.
     * Use the BasicLink to ensure serializable on tablet.
     */
    fun listJointLinks() : List<JointLink> {
        val list = mutableListOf<JointLink>()
        return list
    }
    fun listJointPositions() : List<JointPosition> {
        val list = mutableListOf<JointPosition>()
        for(jp in posmap.values) {
            list.add(jp)
        }
        return list
    }

    fun setOrigin(jp:JointPosition) {
        jp.joint = Joint.IMU
        posmap.put(jp.joint,jp)
        LOGGER.info(String.format("%s.setOrigin: %s",
            CLSS,jp.joint.name))
    }

    // NOTE: It appears that classes with LOGGERs cannot be serialized.
    fun skeletonToJson() :String {
        val gson = GsonBuilder().create()
        return gson.toJson(listJointLinks())
    }

//--------------------------
    fun addJointPosition(jp:JointPosition) {
        posmap.put(jp.joint,jp)
    }

    fun populateFromList(positions:List<JointPosition>) {
        for(pos in positions) {
            posmap.put(pos.joint,pos)
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
                if(jp!=null) jp.setJointAngle(jlink.home)
            }
        }
    }
    /**
     * Populate all joints in the tree to their
     * home angle. This is presuneably the "straight" position.
     */
    fun setJointsToHome() {
        for (jlink in linkmap.values) {
            val jp = getOrCreateJointPosition(jlink.sourceJoint)
            jp.setJointAngle(jlink.home)
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
        var imu = ForwardSolver.tree.getOrCreateJointLink(Joint.IMU)
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
            for(jlink in ForwardSolver.tree.linkmap.values ) {
                if(jlink.sourceJoint==link.endJoint) {
                    list.add(jlink)
                }
            }
        }
        return list
    }

    private val CLSS = "JointTree"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        posmap = mutableMapOf<Joint, JointPosition>()
        linkmap= mutableMapOf<Joint,JointLink>()
        linkSequence = mutableListOf<JointLink>()
    }
}
