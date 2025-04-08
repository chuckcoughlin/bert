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
    private var priorAngle: Double  // Last time Q evaluated

    // The axis is an array of +-1 and two zeros. It describes the joint axis
    // direction (x,y or z) when the robot is in its "straight" or at-reset
    // position.
    var rotation: Double
    // The co-ordinates are position of this end point with respect to the
    // parent (source) joint. x = side to side, y = front-back, z = up/down.
    var coordinates: Point3D
    var endPin:    LinkPin
    var sourcePin: LinkPin

    fun coordinatesToText():String {
        return String.format("%3.3f,%3.3f,%3.3f",coordinates.x,coordinates.y,coordinates.z)
    }

    /** Recalculate quaternion. No need unless angle has changed.
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
        if( !priorAngle.isNaN() && priorAngle==sourcePin.angle) return
        priorAngle = sourcePin.angle
        val angle = priorAngle   // Radians

        // Alpha is the angle of the joint axis with respect to the source axis.
        // Zero degrees implies parallel
        val alpha = rotation
        var theta = angle - sourcePin.home
        if(DEBUG) LOGGER.info(String.format("%s.update: %s angle, home, theta = %2.2f, %2.2f,%2.2f",CLSS,name,angle*180.0/Math.PI,
                                             sourcePin.home*180.0/Math.PI,theta*180.0/Math.PI))
        if(coordinates.z>0.0) {
            theta += Math.atan(coordinates.x / coordinates.z)
            if(DEBUG) LOGGER.info(String.format("%s.update: %s theta now = %2.2f",CLSS,name,theta*180.0/Math.PI))
        }
        //val theta = (if(coordinates.z==0.0) sourcePin.home*Math.PI/180.0 else Math.atan(coordinates.x / coordinates.z)) + angle
        val d = coordinates.y
        val r = Math.sqrt(coordinates.z*coordinates.z + coordinates.x*coordinates.x)
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
        rotation = 0.0                       // Parallel by default
        endPin = LinkPin(PinType.ORIGIN)     // Must be defined
        sourcePin = LinkPin(PinType.ORIGIN)  // Origin until set otherwise.
    }
}