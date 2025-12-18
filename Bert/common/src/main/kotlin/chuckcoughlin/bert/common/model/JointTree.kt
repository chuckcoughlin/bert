/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
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
    val map: MutableMap<Int, JointPosition>
    val linkmap: MutableMap<String, JointLink>  // Key = SOURCE:END
    var IMU:Int

    fun createJointLink(source:JointPosition,jp:JointPosition) : JointLink {
        LOGGER.info(String.format("%s.createJointLink: %s to %s",CLSS,source.name,jp.name))
        val jlink = JointLink(source,jp)
        val key = makeKey(source,jp)
        linkmap.put(key, jlink)
        return jlink
    }

    /**
     * Create a new jpoint position. Add it and a
     * corresponding quaternion to the tree.
     */
    fun createJointPosition(name:String) : JointPosition {
        val jp = JointPosition()
        jp.name = name.uppercase()
        map.put(jp.id,jp)
        return jp
    }
    /**
     * Work back toward the root link beginning with the indicated joint or
     * end effector. The chain starts with the link from root to next joint.
     *
     * @param joint, name of the source.
     * @return a linked list of JointLinks
     */
    fun createLinkChain(joint: String): List<JointLink> {
        val chain: LinkedList<JointLink> = LinkedList<JointLink>()
        var jp= getJointPositionByName(joint)
        do {
            val parent=tree.getParent(jp)
            val jlink= getJointLink(parent.name,jp.name)
            chain.addFirst(jlink)
            // if (DEBUG) LOGGER.info(String.format("%s.createLinkChain: %s - inserting %s (%s)",CLSS,joint.name))
            jp = parent
        } while(jp.parent != ConfigurationConstants.NO_ID)

        return chain
    }

    /**
     * Work back toward the root position beginning with the indicated joint or
     * end effector. The chain starts with the root (IMU) position.
     * @param joint, name of the source.
     * @return a linked list of joint positions
     */
    fun createPositionChain(joint: String): List<JointPosition> {
        val chain: LinkedList<JointPosition> = LinkedList<JointPosition>()
        var jp= getJointPositionByName(joint)
        chain.addFirst(jp)
        // if (DEBUG) LOGGER.info(String.format("%s.createPositionChain: %s - chain to %s (%s)",CLSS,joint.name))
        do {
            jp= getParent(jp)
            chain.addFirst(jp)
            // if (DEBUG) LOGGER.info(String.format("%s.createPositionChain: %s - inserting %s (%s)",CLSS,joint.name))
        } while(jp.parent != ConfigurationConstants.NO_ID)

        return chain
    }

    fun getJointLink(source:String,end:String) : JointLink {
        //LOGGER.info(String.format("%s.getJointLink: %d",CLSS,id))
        val key = makeKey(source,end)
        val jlink = linkmap.get(key)
        if(jlink==null ) {
            LOGGER.warning((String.format("%s.getJointLink: No link found for %s - created",CLSS,key)))
            return JointLink(getJointPositionByName(source),getJointPositionByName(end))
        }
        return jlink
    }

    /**
     * If the named position does not exist, create one.
     * The name is case-insensitive.
     */
    fun getJointPositionByName(name:String) : JointPosition {
        // LOGGER.info(String.format("%s.getJointPositionByName: %s",CLSS,name))
        for( jp in map.values ) {
            if( jp.name.equals(name,true)) return jp
        }
        val jp = createJointPosition(name)
        return jp
    }

    fun listJointPositions() : List<JointPosition> {
        val list = mutableListOf<JointPosition>()
        for(jp in map.values) {
            list.add(jp)
        }
        return list
    }

    fun getOrigin():JointPosition {
        val origin = map.get(IMU)!!
        return origin
    }


    /**
     * @return the parent joint position. If the position
     *         does not exist, return the origin.
     */
    fun getParent(jp:JointPosition) : JointPosition {
        var parent = map.get(jp.parent)
        if( parent==null ) parent = map.get(IMU)
        return parent!!
    }

    fun setOrigin(jp:JointPosition) {
        IMU= jp.id
        map.put(jp.id,jp)
        LOGGER.info(String.format("%s.setOrigin: %s = (%d)",
            CLSS,jp.name,jp.id))
    }

//--------------------------


    fun addJointPosition(jp:JointPosition) {
        map.put(jp.id,jp)
    }

    fun populateFromList(positions:List<JointPosition>) {
        for(pos in positions) {
            map.put(pos.id,pos)
        }
    }
    fun clone() : JointTree {
        val copy = JointTree()
        for(key in map.keys) {
            val jp = map.get(key)!!.copy()
            copy.map.put(key,jp)
        }
        for(key in linkmap.keys) {
            val jlink = linkmap.get(key)!!.clone()
            copy.linkmap.put(key,jlink)
        }
        copy.IMU = IMU
        return copy
    }

    private fun makeKey(source:String,end:String) :String {
        val key = source+":"+end
        return key
    }
    private fun makeKey(source:JointPosition,end:JointPosition) :String {
        val key = source.name+":"+end.name
        return key
    }

    private val CLSS = "JointTree"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        map = mutableMapOf<Int, JointPosition>()
        linkmap = mutableMapOf<String,JointLink>()
        IMU = ConfigurationConstants.NO_ID // Temporarily
    }
}
