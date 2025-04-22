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
    private var priorRoll: Double  // Last time Q evaluated
    private var priorPitch: Double  // Last time Q evaluated
    private var priorYaw: Double  // Last time Q evaluated

    var endPin:    LinkPin
    var sourcePin: LinkPin

    fun coordinatesToText():String {
        return String.format("%3.3f,%3.3f,%3.3f",coordinates.x,coordinates.y,coordinates.z)
    }

    fun setCoordinates(x:Double,y:Double,z:Double) {
        quaternion.setTranslation(x,y,z)
    }
    fun setRoll(x:Double) {
        quaternion.setRoll(angle)
    }
    fun setPitch(angle:Double) {
        quaternion.setPitch(x,y,z)
    }
    fun setYaw(angle:Double) {
        quaternion.setYaw(angle)
    }
    fun setRpy(roll:Double,pitch:Double,yaw:Double) {
        rpy[0] = roll*Math.PI/180.0
        rpy[1] = pitch*Math.PI/180.0
        rpy[2] = yaw*Math.PI/180.0
        quaternion.setRoll(roll)
        quaternion.setPitch(pitch)
        quaternion.setYaw(yaw)
        quaternion.update()
    }

    /** Recalculate quaternion. No need unless angle has changed.
     * Note. joint coordinates are with respect robot reference frame
     *       when straight. "z" is up, "x" is front, "y" is across.
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
     * Distances _mm, angle in radians
     */
    fun update() {
        // No work if we haven't moved since last update

        // Create a common nornmal between source z and end z
        // New x is from source z along common normal
        //

        // **** Different combinations of motor axis alignments ****
        if( sourcePin.axis==Axis.X && endPin.axis==Axis.X ) {
        }
        else {
            LOGGER.warning(String.format("%s.update: %s No code to juxtapose %s vs %s",CLSS,name,sourcePin.axis.name,
                                          endPin.axis.name))
        }

        if(DEBUG) LOGGER.info(String.format("%s.update: %s %s->%s source angle,end angle,alpha,theta,d,r =  %2.2f,%2.2f,%2.2f,%2.2f",CLSS,name,
                            sourcePin.axis.name,endPin.axis.name,
                            sourcePin.angle*180.0/Math.PI,endPin.angle*180.0/Math.PI,
                            alpha*180.0/Math.PI,theta*180.0/Math.PI))

        quaternion.update()
    }

    private val CLSS = "Link"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean
    /**
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        priorRoll = Double.NaN
        priorPitch = Double.NaN
        priorYaw = Double.NaN
        quaternion = Quaternion()
        endPin = LinkPin(PinType.ORIGIN)     // Must be defined
        sourcePin = LinkPin(PinType.ORIGIN)  // Origin until set otherwise.
    }
}