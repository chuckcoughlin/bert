/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.solver.ForwardSolver.tree
import java.util.*
import java.util.logging.Logger

/**
 * Retain a tree of linked joint positions. Each joint is
 * associated with a quaternion for computing 3D co-ordinates.
 */
class JointTree() {
    val posmap: MutableMap<Joint, JointPosition>
    val linkmap: MutableMap<Joint, JointLink>  // Key = endJoint

    fun createJointLink(source:Joint,jp:Joint) : JointLink {
        LOGGER.info(String.format("%s.createJointLink: %s to %s",CLSS,source.name,jp.name))
        val jlink = JointLink(source,jp)
        linkmap.put(jp, jlink)
        return jlink
    }

    /**
     * Create a new joint position. Add it to the tree.
     */
    fun createJointPosition(joint:Joint,parent:JointPosition) : JointPosition {
        val jp = JointPosition()
        jp.joint = joint
        jp.parent = parent
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
    fun createLinkChain(joint: Joint): List<JointLink> {
        val chain: LinkedList<JointLink> = LinkedList<JointLink>()
        var jp= getOrCreateJointPosition(joint)
        do {
            val parent=tree.getParent(jp)
            val jlink= getJointLink(jp.joint)
            chain.addFirst(jlink)
            // if (DEBUG) LOGGER.info(String.format("%s.createLinkChain: %s - inserting %s (%s)",CLSS,joint.name))
            jp = parent
        } while(jp.parent != JointPosition.NONE)

        return chain
    }

    /**
     * Work back toward the root position beginning with the indicated joint or
     * end effector. The chain starts with the root (IMU) position.
     * @param joint, name of the source.
     * @return a linked list of joint positions
     */
    fun createPositionChain(joint: Joint): List<JointPosition> {
        val chain: LinkedList<JointPosition> = LinkedList<JointPosition>()
        var jp= getOrCreateJointPosition(joint)
        chain.addFirst(jp)
        // if (DEBUG) LOGGER.info(String.format("%s.createPositionChain: %s - chain to %s (%s)",CLSS,joint.name))
        do {
            jp= getParent(jp)
            chain.addFirst(jp)
            // if (DEBUG) LOGGER.info(String.format("%s.createPositionChain: %s - inserting %s (%s)",CLSS,joint.name))
        } while(jp.parent != JointPosition.NONE)

        return chain
    }

    fun getJointLink(end:Joint) : JointLink {
        //LOGGER.info(String.format("%s.getJointLink: %d",CLSS,id))
        val jlink = linkmap.get(end)
        if( jlink==null ) {
            LOGGER.warning(String.format("%s.getJointLink: No link found for endJoint %s - created",CLSS,end.name))
            val jp = posmap.get(end)
            if(jp!=null) {
                val parent = jp.parent.joint
                return JointLink(getOrCreateJointPosition(parent).joint,end)
            }
            return JointLink(Joint.IMU,end)
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
            jp = createJointPosition(joint,JointPosition.NONE)
        }
        return jp
    }

    fun listJointPositions() : List<JointPosition> {
        val list = mutableListOf<JointPosition>()
        for(jp in posmap.values) {
            list.add(jp)
        }
        return list
    }

    fun getOrigin():JointPosition {
        val origin = posmap.get(Joint.IMU)!!
        return origin
    }


    /**
     * @return the parent joint position. If the position
     *         does not exist, return the origin.
     */
    fun getParent(jp:JointPosition) : JointPosition {
        var parent = jp.parent
        return parent
    }

    fun setOrigin(jp:JointPosition) {
        jp.joint = Joint.IMU
        jp.parent= JointPosition.NONE
        posmap.put(jp.joint,jp)
        LOGGER.info(String.format("%s.setOrigin: %s",
            CLSS,jp.joint.name))
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
        return copy
    }

    private val CLSS = "JointTree"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        posmap = mutableMapOf<Joint, JointPosition>()
        linkmap= mutableMapOf<Joint,JointLink>()
    }
}
