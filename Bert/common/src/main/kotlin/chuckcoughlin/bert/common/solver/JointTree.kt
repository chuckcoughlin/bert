/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.solver

import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointLink
import chuckcoughlin.bert.common.model.JointPosition

/**
 * Retain a tree of linked joint positions. Each joint is
 * associated with a quaternion for computing 3D co-ordinates.
 */
class JointTree() {
    val map: MutableMap<Int, JointPosition>
    val linkmap: MutableMap<Int, JointLink>
    var IMU:Int

    fun createJointLink(jp:JointPosition,source:JointPosition) : JointLink {
        val jlink = JointLink(jp,source)
        linkmap.put(jp.id, jlink)
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

    fun getJointLinkById(id:Int) : JointLink {
        val jlink = linkmap.get(id)
        return jlink!!
    }

    /**
     * If the named position does not exist, create one.
     * The name is case-insensitive.
     */
    fun getJointPositionByName(name:String) : JointPosition {
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

    private val CLSS = "JointTree"

    init {
        map = mutableMapOf<Int, JointPosition>()
        linkmap = mutableMapOf<Int,JointLink>()
        IMU= ConfigurationConstants.NO_ID // Temporarily
    }
}
