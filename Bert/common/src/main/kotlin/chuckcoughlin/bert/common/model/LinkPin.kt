/**
 * Copyright 2024-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model


/**
 * A LinkPoint is either a hinged joint or a fixed extremity.
 * The coordinates are a 3D location of the joint/extremity
 * with respect to the origin of the link.
 *
 * The orientation array shows the direction of the
 * axis of the joint with respect a line from the joint
 * to the link origin. The offset coordinates are with respect
 * to the link origin. In most cases, the linkPoint
 * is along the z axis.
 */
class LinkPin {
    var offset : DoubleArray
    var orientation: DoubleArray
    val extremity: Extremity
    val joint: Joint
    val type:PinType

    /**
     * Special constructor for a LinkPoint representing the origin of the link chain.
     */
    constructor() {
        type = PinType.ORIGIN
        extremity = Extremity.NONE
        joint = Joint.NONE
        offset = doubleArrayOf(0.0, 0.0, 0.0)
        orientation = doubleArrayOf(0.0, 0.0, 0.0)
    }
    constructor(ext: Extremity, rot: DoubleArray, pos: DoubleArray) {
        type = PinType.EXTREMITY
        extremity = ext
        joint = Joint.NONE
        offset = pos
        orientation = degreesToRadians(rot)
    }

    constructor(j: Joint, rot: DoubleArray, pos: DoubleArray) {
        type = PinType.REVOLUTE
        extremity = Extremity.NONE
        joint = j
        orientation = degreesToRadians(rot)
        offset = pos
    }

    private fun degreesToRadians(array: DoubleArray): DoubleArray {
            var i = 0
            while (i < array.size) {
                array[i] = array[i] * Math.PI / 180.0
                i++
            }
        return array
    }

    private val CLSS =  "LinkPoint"

}