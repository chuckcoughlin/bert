/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bertspeak.common.ConfigurationConstants
import chuckcoughlin.bertspeak.data.JointPosition

/**
 * Parse the URDF file and make its contents available as a chain of links.
 * A chain has a tree structure with a single root.
 */
class JointTree() {
    val map: MutableMap<Int, JointPosition>
    val ERROR:JointPosition
    val IMU:JointPosition

    fun clear() {
        map.clear()
    }

    fun addJointPosition(jp:JointPosition) {
        map.put(jp.id,jp)
    }

    fun getParent(jp:JointPosition) : JointPosition {
        val parent = map.get(jp.parent)
        if( parent!=null ) return parent
        return ERROR
    }
    fun getPositionByName(name:String) : JointPosition {
        for( jp in map.values ) {
            if( jp.name==name) return jp
        }
        return ERROR
    }

    // @return the IMU to signal the root of a chain.
    fun nextPosition(position:JointPosition) : JointPosition {
        var next = map.get(position.id)
        if(next==null) next = IMU
        return next
    }

    fun populateFromList(positions:List<JointPosition>) {
        for(pos in positions) {
            map.put(pos.id,pos)
        }
    }

    // This works for joints by name also
    fun positionForAppendage(name:String) : JointPosition {
        for(pos in map.values) {
            if(pos.name.equals(name)) return pos
        }
        return ERROR
    }

    private val CLSS = "JointTree"

    init {
        map = mutableMapOf<Int, JointPosition>()
        IMU = JointPosition()
        IMU.id = ConfigurationConstants.NO_ID
        IMU.name = Joint.IMU.name

        ERROR = JointPosition()
        ERROR.id = ConfigurationConstants.NO_ID
        ERROR.name = Joint.NONE.name

    }
}
