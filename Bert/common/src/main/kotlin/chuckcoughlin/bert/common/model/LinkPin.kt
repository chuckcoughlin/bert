/**
 * Copyright 2024-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import java.awt.Robot
import java.util.logging.Logger

/**
 * A LinkPoint is either a hinged joint or an end effector (unmoving).
 * The offset coordinates are a 3D location of the joint/end effector
 * with respect to the origin of the link. The origin of a Link is
 * a REVOLUTE LinkPin belonging to the parent of that link.
 *
 * The alignment array is a unit-length vector showing the direction of the
 * axis of the joint motor with respect to a line from the joint
 * to the link origin. The alignment coordinates are with respect
 * to the link origin. In most cases, the linkPin
 * is along the z axis.
 */
class LinkPin (val type:PinType ) {
    val quaternion: Quaternion
    var appendage: Appendage  // End effector
    // The axis represents the orientation of the link
    // with respect to the robot's inertial frame.
    var axis: DoubleArray
    // The co-ordinates are position of this end point with respect to the
    // parent (source) joint. X is the direction from source to the end point.
    // Z is the center of the parent joint. Co-ordinates are NOT used
    // for kinematics calculations.
    var coordinates: Point3D
    var mc: MotorConfiguration? = null
    var offset: Double  // joint angle equivalent to "straight"

    /*
     * The joint implies a motor configuration. From it we get the rotational angle.
     * Note that changing the angle does not invalidate the current link, just its children.
     */
    var joint: Joint
        get() =
            if (mc == null) Joint.NONE
            else mc!!.joint

        set(j) {
            mc = RobotModel.motorsByJoint[j]
        }
    /**
     * The joint angle is the motor position in degrees.
     * Note that changing the angle does not invalidate the current link, just its children.
     * @return
     */// Convert to radians
    var theta:Double = 0.0
        get() = (if(coordinates.x==0.0) offset else Math.atan(coordinates.y / coordinates.x) * 180.0/Math.PI) + (if(mc==null) 0.0 else mc!!.angle) - offset
    fun coordinatesToText():String {
        return String.format("%3.3f,%3.3f,%3.3f",coordinates.x,coordinates.y,coordinates.z)
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
    fun updateQuaternion() {
        quaternion.update(coordinates.z,coordinates.x,0.0,theta)
    }

    companion object {
        /**
         * @return a new link pin that serves the special function
         *         of being the origin. It's position is always the same
         *         (0,0,0), but its orientation may vary
         */
        fun imu(): LinkPin {
            val pin = LinkPin(PinType.ORIGIN)
            return pin
        }
    }

    val DEBUG: Boolean
    private val CLSS =  "LinkPin"
    val LOGGER = Logger.getLogger(CLSS)

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        appendage = Appendage.NONE
        quaternion = Quaternion()
        coordinates = Point3D(0.0, 0.0, 0.0)   // x,y,z
        axis = doubleArrayOf(0.0,0.0,0.0)
        offset = 0.0
    }

}