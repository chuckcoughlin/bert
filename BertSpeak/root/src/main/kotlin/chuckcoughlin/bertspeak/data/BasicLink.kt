
/**
 * Copyright 2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bertspeak.ui.graphics.Side
import java.util.logging.Logger

/**
 * A link is a skeletal structure beginning with a source joint and ending
 * with either another joint or end effector. Values are static. The joint in the
 * source is modelled as rotation the entire link structure as a unit. The
 * "orientation" refers to the fixed orientation of tne source joint to the
 * previous limb when the joint angle is at zero with respect to the previous link. The
 * "coordinates" refer to the location of the end joint or end-effector with
 * respect to the source. The axes for these coordinates are always "y" corresponding
 * to the axis of the source joint, "x" is positive forward, "z" is up.
 *
 * Multiple joint-links may have the same source, indicating they are on the same physical
 * skeletal piece. A link may be uniquely identified by its end joint/effector.
 *
 * @param source the source joint
 * @param end the end joint or end effector
 */
open class BasicLink(source:Joint,end:Joint) {
    // Values are degrees.
    var orientation:DoubleArray
    var coordinates:DoubleArray
    val endJoint:Joint
    val sourceJoint:Joint
    var side:String

    // These are the physical fixed distances between source
    // and end joint from the URDF file. The joint angle
    // is in its home position. Initialize position to same.
    // ~mm
    open fun setCoordinates(x:Double,y:Double,z:Double) {
        coordinates[0] = x
        coordinates[1] = y
        coordinates[2] = z
    }

    // Roll, pitch, yaw are in degrees. Convert to radians.
    // This refers to the orientation of the origin
    // with respect to the previous link. ~ degrees
    open fun setRpy(roll:Double,pitch:Double,yaw:Double) {
        orientation[0] = roll
        orientation[1] = pitch
        orientation[2] = yaw
    }

    init {
        sourceJoint = source
        endJoint    = end
        coordinates = doubleArrayOf(0.0,0.0,0.0)  // end referenced to source
        orientation = doubleArrayOf(0.0,0.0,0.0)
        side = Side.FRONT.name
    }
}
