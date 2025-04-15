/**
 * Copyright 2023-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Axis
import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.model.IMU.alpha
import chuckcoughlin.bert.common.model.IMU.coordinates
import chuckcoughlin.bert.common.model.IMU.quaternion
import chuckcoughlin.bert.common.model.IMU.theta
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
    private var priorAngle: Double  // Last time Q evaluated

    // The co-ordinates are position of this end point with respect to the
    // parent (source) joint. x = side to side, y = front-back, z = up/down.
    var coordinates: Point3D
    var endPin:    LinkPin
    var sourcePin: LinkPin

    fun coordinatesToText():String {
        return String.format("%3.3f,%3.3f,%3.3f",coordinates.x,coordinates.y,coordinates.z)
    }

    /** Recalculate quaternion. No need unless angle has changed.
     * Note. joint coordinates are with respect robot reference frame
     *       when straight. "z" is up, "x" is across, "y" is front.
     *
     * Convert to coordinate system of the quaternion.
     * Z is the rotational axis of the parent joint.
     * X points to the next link.
     * Y completes the right-hand coordinate system.
     *
     * In our robot, adjacent motors (joints) are either eligned with parallel
     * axes or at right angles to each other as defined by the "rotation" angle.
     * Refer to "How to Calculate a Robot's Forward Kinematics in 5 Easy Steps"
     * by Alex Owen-Hill, these are the equivalents to our coordinate matrix:
     *
     * d offset to source along z
     * r distance to source along common normal
     * alpha angle around common normal between source and current z
     * theta: angle of rotation of joint
     *
     * Distances _mm, angle in radians
     */
    fun update() {
        // No work if we haven't moved since last update
        if( !priorAngle.isNaN() && priorAngle==endPin.angle) return
        priorAngle = endPin.angle

        var alpha = 0.0
        var d = 0.0
        var r = 0.0
        var theta = 0.0

        // Degenerate case. Motors on top of each other.
        if(coordinates.z<=0.0) {
            theta = sourcePin.angle
        }
        // Different cases depending an motor axis alignments
        else if( sourcePin.axis==Axis.X && endPin.axis==Axis.X ) {
            alpha = 0.0
            d = coordinates.y
            r = Math.sqrt(coordinates.z*coordinates.z + coordinates.x*coordinates.x)
            theta = Math.atan(coordinates.x / coordinates.z) + sourcePin.angle
        }
        else if( sourcePin.axis==Axis.X && endPin.axis==Axis.Y ) {
            alpha = Math.PI/2.0
            d = coordinates.y
            r = Math.sqrt(coordinates.z*coordinates.z + coordinates.x*coordinates.x)
            theta = Math.atan(coordinates.x / coordinates.z) + sourcePin.angle
            theta = 0.0  // Gives right answer for abs y
        }
        else if( sourcePin.axis==Axis.Y && endPin.axis==Axis.Z ) {
            alpha = sourcePin.angle
            d = coordinates.y
            r = Math.sqrt(coordinates.z*coordinates.z + coordinates.x*coordinates.x)
            theta = Math.atan(coordinates.x / coordinates.z)
        }
        else {
            LOGGER.warning(String.format("%s.update: %s No code to juxtapose %s vs %s",CLSS,name,sourcePin.axis.name,
                                          endPin.axis.name))
        }

        if(DEBUG) LOGGER.info(String.format("%s.update: %s %s->%s source angle,end angle,alpha,theta,d,r =  %2.2f,%2.2f,%2.2f,%2.2f,%2.2f,%2.2f",CLSS,name,
                            sourcePin.axis.name,endPin.axis.name,
                            sourcePin.angle*180.0/Math.PI,endPin.angle*180.0/Math.PI,
                            alpha*180.0/Math.PI,theta*180.0/Math.PI,d,r))

        quaternion.update(d,r,alpha,theta)
    }

    private val CLSS = "Link"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean
    /**
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        priorAngle = Double.NaN
        quaternion = Quaternion()
        coordinates = Point3D(0.0, 0.0, 0.0)   // x,y,z
        endPin = LinkPin(PinType.ORIGIN)     // Must be defined
        sourcePin = LinkPin(PinType.ORIGIN)  // Origin until set otherwise.
    }
}