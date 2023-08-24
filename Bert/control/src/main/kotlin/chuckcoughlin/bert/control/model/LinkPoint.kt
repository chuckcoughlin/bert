/**
 * Copyright 2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.control.model

import chuckcoughlin.bert.common.model.Appendage
import chuckcoughlin.bert.common.model.Joint


/**
 * A LinkPoint is a hinged joint (as they all are).
 * The coordinates are a 3D location of the joint
 * with respect to the origin of the link.
 *
 * The orientation array shows the direction of the
 * axis of the joint with respect a line from the joint
 * to the link origin. The offset coordinates are with respect
 * to the link origin. In most cases, the linkPoint
 * is along the z axis.
 */
class LinkPoint {
    val name: String
        get() = joint.name
    var offset : DoubleArray // Joint offset
        private set
    var orientation: DoubleArray
    val type: LinkPointType
    val appendage: Appendage
    val joint: Joint

    /**
     * Special constructor for
     * a LinkPoint representing the origin of the link chain.
     */
    constructor() {
        type = LinkPointType.ORIGIN
        appendage = Appendage.NONE
        joint = Joint.NONE
        offset = doubleArrayOf(0.0, 0.0, 0.0)
        orientation = doubleArrayOf(0.0, 0.0, 0.0)
    }
    constructor(app: Appendage, rot: DoubleArray, pos: DoubleArray) {
        type = LinkPointType.APPENDAGE
        appendage = app
        joint = Joint.NONE
        offset = pos
        orientation = degreesToRadians(rot)
    }

    constructor(j: Joint, rot: DoubleArray, pos: DoubleArray) {
        type = LinkPointType.REVOLUTE
        appendage = Appendage.NONE
        joint = j
        offset = pos
        orientation = degreesToRadians(rot)
    }

    private fun degreesToRadians(array: DoubleArray): DoubleArray {
            var i = 0
            while (i < array.size) {
                array[i] = array[i] * Math.PI / 180.0
                i++
            }
        return array
    }

    companion object {
        private const val CLSS = "LinkPoint"
    }
}