/**
 * Copyright 2023-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import java.util.logging.Logger

/**
 * A link is a skeletal structure connecting a source pin to either a
 * revolute or end effector. The "axis" and "coordinates" members are
 * static and refer to the orientation and position of the end pin with respect
 * to the source pin. The "position" of the link is the position of the
 * end point. It is a dynamic quantity derived from the quaternion matrix.
 * It represents distances and orientation with respect to the robot
 * inertial frame.
 *
 * If the end pin represents a joint, then the pin is a "revolute", that is
 * rotational only. There is no translation. The joint axis is always at right
 * angles to a line from the source to end.  Multiple links may have the same source.
 *
 * The end pin may also represent an "extremity" or "end effector". We call it an
 * "appendage". An appendage is not moveable, but we can calculate its 3D location
 * and orientation.
 *
 * Multiple links may have the same source, indicating they are on the same physical
 * skeletal piece.
 *
 * @param type link type
 */
class Link( val nam:String ) {
    val name = nam
    val quaternion: Quaternion

    // The orientation of the link is a unit vector
    // aligning with the robot's inertial frame.
    var orientation: DoubleArray
    // The co-ordinates are position of this end point with respect to the
    // parent (source) joint. X is the direction from source to the end point.
    // Z is the center of the parent joint.
    var coordinates: Point3D
    var endPin:    LinkPin
    var sourcePin: LinkPin

    var angle:Double = -1.0
        get() = field
        set(a) {
            if( field!=a) {
                field = a
                updateQuaternion()
            }
        }

    fun coordinatesToText():String {
        return String.format("%3.3f,%3.3f,%3.3f",coordinates.x,coordinates.y,coordinates.z)
    }
    fun updateJointPosition() {
        angle = sourcePin.angle
    }

    /**
     * In referring to the article "How to Calculate a Robot's Forward Kinematics in 5 Easy Steps"
     * by Alex Owen-Hill, these are the equivalents to our coordinate matrix:
     *
     * d = z
     * r = x
     * alpha = 0 (for parallel motors)
     * thata = arctan(y/x)
     */
     private fun updateQuaternion() {
        // The joint angle is the motor position in degrees - convert to radians.
        val theta = (if(coordinates.x==0.0) sourcePin.offset else Math.atan(coordinates.y / coordinates.x) * 180.0/Math.PI) + angle
        quaternion.update(coordinates.z,coordinates.x,0.0,theta)
    }

    private fun rotationFromCoordinates(cc: DoubleArray): DoubleArray {
        var len = Math.sqrt(cc[0] * cc[0] + cc[1] * cc[1] + cc[2] * cc[2])
        if (len == 0.0) len = 1.0 // All angles will be 90 deg
        val rot = DoubleArray(3)
        rot[0] = Math.acos(cc[0] / len)
        rot[0] = Math.acos(cc[1] / len)
        rot[0] = Math.acos(cc[2] / len)
        return rot
    }

    private val CLSS = "Link"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean
    /**
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        angle = -1.0
        quaternion = Quaternion()
        coordinates = Point3D(0.0, 0.0, 0.0)   // x,y,z
        orientation = doubleArrayOf(0.0,0.0,0.0)
        endPin = LinkPin(PinType.ORIGIN)     // Must be reset
        sourcePin = LinkPin(PinType.ORIGIN)  // Origin until set otherwise.
    }
}