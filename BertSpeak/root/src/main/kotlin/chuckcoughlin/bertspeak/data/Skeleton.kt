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
    val positionMap: MutableMap<Joint, JointPosition>
    val linkMap: MutableMap<Joint, BasicLink>

    fun clear() {
        linkMap.clear()
        positionMap.clear()
    }

    fun addJointLink(jl:BasicLink) {
        linkMap.put(jl.endJoint,jl)
    }

    fun addJointPosition(jp:JointPosition) {
        positionMap.put(jp.joint,jp)
    }
    fun getPositionByJoint(joint:Joint) : JointPosition {
        val jp = positionMap.get(joint)
            if( jp==null) return JointPosition.NONE
        return jp
    }

    fun populateFromList(positions:List<JointPosition>) {
        for(pos in positions) {
            positionMap.put(pos.joint,pos)
        }
    }

    private val CLSS = "Skeleton"

    init {
        positionMap = mutableMapOf<Joint, JointPosition>()
        linkMap = mutableMapOf<Joint, BasicLink>()
    }
}
