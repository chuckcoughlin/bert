/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

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
        } while(jp.joint != Joint.NONE)

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
    fun listJointLinks() : List<BasicLink> {
        val list = mutableListOf<BasicLink>()
        for(link in linkmap.values ) {
            val bl = BasicLink(link.sourceJoint,link.endJoint)
            bl.coordinates = link.coordinates.clone()
            bl.orientation = link.orientation.clone()
            list.add(bl)
        }
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
