/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.solver

import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointPosition
import chuckcoughlin.bert.common.model.URDFModel
import chuckcoughlin.bert.common.solver.ForwardSolver.tree
import com.google.gson.GsonBuilder
import jdk.javadoc.internal.tool.Start


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

    /**
     * Populate the tree with current joint positions
     * based on the URDFModel
     */
    private fun initialize() {
        // Start with the end effectors
        for(link in URDFModel.appendageLinks.values) {
            val jp = JointPosition()
            jp.updateFromLink(link)
            jp.isAppendage = true
            var pos = ForwardSolver.computePosition(link.sourcePin.joint)
            jp.updateSource(pos)os)
            pos = ForwardSolver.computePosition(link.endPin.appendage)
            jp.updateEnd(p
            if(ForwardSolver.DEBUG) ForwardSolver.LOGGER.info(String.format("%s.fillLocations: %s = (%s) ",
                ForwardSolver.CLSS,link.endPin.appendage.name, jp.positionToText()))
            tree.addJointPosition(jp)
        }
        for(link in URDFModel.jointLinks.values) {
            val jp = JointPosition()
            jp.updateFromLink(link)
            jp.isAppendage = false
            var pos = ForwardSolver.computePosition(link.sourcePin.joint)
            loc.updateSource(pos)
            pos = ForwardSolver.computePosition(link.endPin.joint)
            loc.updateEnd(pos)
            tree.addJointPosition(jp)
        }
    }

    fun addJointPosition(jp:JointPosition) {
        map.put(jp.id,jp)
    }

    fun getParent(jp:JointPosition) : JointPosition {
        val parent = map.get(jp.parent)
        if( parent!=null ) return parent
        return ERROR
    }

    /**
     * If the named position does not exist, crease one.
     */
    fun getPositionByName(name:String) : JointPosition {
        for( jp in map.values ) {
            if( jp.name==name) return jp
        }
        return ERROR
    }

    /**
     *
     */
    fun linkPositionsToJSON():String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val tree = JointTree()
        ForwardSolver.fillPositions(tree)
        return gson.toJson(tree.map.values)
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
        initialize()
        map = mutableMapOf<Int, JointPosition>()
        IMU = JointPosition()
        IMU.id = ConfigurationConstants.NO_ID
        IMU.name = Joint.IMU.name

        ERROR = JointPosition()
        ERROR.id = ConfigurationConstants.NO_ID
        ERROR.name = Joint.NONE.name

    }
}
