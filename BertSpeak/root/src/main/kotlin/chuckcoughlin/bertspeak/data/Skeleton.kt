/**
 * Copyright 2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bert.common.model.Joint

/**
 * This is an abbreviated version of a JointTree
 * in the main app.
 */
class Skeleton() {
    val sideMap: MutableMap<Joint,Side>
    val positionMap: MutableMap<Joint, JointPosition>
    val linkMap: MutableMap<Joint, JointLink>

    fun clear() {
        linkMap.clear()
        positionMap.clear()
    }

    fun addJointLink(jl:JointLink) {
        linkMap.put(jl.endJoint,jl)
        sideMap.put(jl.endJoint,Side.fromString(jl.side))
    }

    fun addJointPosition(jp:JointPosition) {
        positionMap.put(jp.joint,jp)
    }
    fun positionForJoint(joint:Joint) : JointPosition {
        val jp = positionMap.get(joint)
        if( jp==null) return JointPosition.NONE
        return jp
    }

    fun sideForJoint(joint:Joint) : Side {
        val side =  sideMap.get(joint)
        if( side==null) return Side.FRONT
        return side
    }

    fun populateFromList(positions:List<JointPosition>) {
        for(pos in positions) {
            positionMap.put(pos.joint,pos)
        }
    }

    private val CLSS = "Skeleton"

    init {
        sideMap = mutableMapOf<Joint, Side>()
        positionMap = mutableMapOf<Joint, JointPosition>()
        linkMap = mutableMapOf<Joint, JointLink>()
    }
}
