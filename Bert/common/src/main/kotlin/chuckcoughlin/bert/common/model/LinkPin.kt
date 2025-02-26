/**
 * Copyright 2024-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * A LinkPoint is either a hinged joint or a fixed extremity.
 * The offset coordinates are a 3D location of the joint/extremity
 * with respect to the origin of the link. The orgin of a Link is
 * a LinkPin belonging to the parent of that link.
 *
 * The alignment array is a unit-length vector showing the direction of the
 * axis of the joint motor with respect to a line from the joint
 * to the link origin. The alignment coordinates are with respect
 * to the link origin. In most cases, the linkPin
 * is along the z axis.
 */
class LinkPin (val type:PinType ) {
    var offset : DoubleArray
    var axis: DoubleArray
    var extremity: Extremity
    var joint: Joint

    fun degreesToRadians(array: DoubleArray): DoubleArray {
        var i = 0
        while (i < array.size) {
            array[i] = array[i] * Math.PI / 180.0
            i++
        }
        return array
    }

    private val CLSS =  "LinkPin"

    init {
        extremity = Extremity.NONE
        joint = Joint.NONE
        offset = doubleArrayOf(0.0, 0.0, 0.0)
        axis = doubleArrayOf(0.0, 0.0, 0.0)    // x,y,z axes
    }

}