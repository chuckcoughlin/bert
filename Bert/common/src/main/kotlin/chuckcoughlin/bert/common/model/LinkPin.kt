/**
 * Copyright 2024-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.util.Quaternion

/**
 * A LinkPoint is either a hinged joint or an end effector (unmoving).
 * The offset coordinates are a 3D location of the joint/end effector
 * with respect to the origin of the link. The orgin of a Link is
 * a REVOLUTE LinkPin belonging to the parent of that link.
 *
 * The alignment array is a unit-length vector showing the direction of the
 * axis of the joint motor with respect to a line from the joint
 * to the link origin. The alignment coordinates are with respect
 * to the link origin. In most cases, the linkPin
 * is along the z axis.
 */
class LinkPin (val type:PinType ) {
    var quaternion: Quaternion
    var appendage: Appendage  // End effector
    var joint: Joint

    fun setDistance(dist:Double) {
        quaternion.q[2][3] = dist
    }

    fun setOrientation(axis:DoubleArray) {
        if( axis.size==3) {

        }
        else {

        }

    }

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
        appendage = Appendage.NONE
        joint = Joint.NONE
        quaternion = Quaternion()
    }

}