/**
 * Copyright 2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bert.common.model.Joint

/**
 * This is an abbreviated version of a class of the same name
 * in the main app.
 */
class JointTree() {
    val map: MutableMap<Joint, JointPosition>

    fun clear() {
        map.clear()
    }

    fun addJointPosition(jp:JointPosition) {
        map.put(jp.joint,jp)
    }
    fun getPositionByJoint(joint:Joint) : JointPosition {
        val jp = map.get(joint)
            if( jp==null) return JointPosition.NONE
        return jp
    }

    fun populateFromList(positions:List<JointPosition>) {
        for(pos in positions) {
            map.put(pos.joint,pos)
        }
    }

    private val CLSS = "JointTree"

    init {
        map = mutableMapOf<Joint, JointPosition>()
    }
}
