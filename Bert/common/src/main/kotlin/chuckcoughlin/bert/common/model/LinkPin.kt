/**
 * Copyright 2024-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion

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

    private val CLSS =  "LinkPin"

    init {
        appendage = Appendage.NONE
        joint = Joint.NONE
        quaternion = Quaternion()
    }

}