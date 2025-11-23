/**
 * Copyright 2023-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import java.util.logging.Logger

/**
 * A link is a skeletal structure connecting a source pin to either a
 * revolute joint or end effector. The "coordinates" and "rotation" members are
 * static and refer to the orientation and position of the end pin with respect
 * to the source pin. The position of the link are the coordinates of the
 * end point. Positions are dynamic, derived from the quaternion matrix.
 * It represents distances and orientation with respect to the robot
 * inertial frame.
 *
 * If the end pin represents a joint, then the pin is a "revolute", that is
 * rotational only. The only translations are the fixed distances between source
 * and end pins. The joint x-axis is always at right angles to a line from the
 * source to end.
 *
 * The end pin may also represent an "extremity" or "end effector". We call it an
 * "appendage". An appendage is not movable, but we can calculate its 3D location
 * and orientation.
 *
 * Multiple links may have the same source, indicating they are on the same physical
 * skeletal piece.
 *
 * @param name link name - either the appendage or joint name
 */
class Link( val name:String ) {
    val quaternion: Quaternion
    // Current angle was in effect last time Q evaluated.
    // Values are degrees.
    private var currentAngle: Double  // Motor angle
    private var rotation:DoubleArray
    var endPin:    LinkPin
    var sourcePin: LinkPin
    var side:Side

    // ~mm
    fun setCoordinates(x:Double,y:Double,z:Double) {
        if(DEBUG) LOGGER.info(String.format("%s.setCoordinates: (%s) %2.2f,%2.2f,%2.2f",CLSS,name,x,y,z))
        quaternion.setTranslation(x,y,z)
        quaternion.update()
    }
    // ~ degrees
    fun setRoll(angle:Double) {
        rotation[0] = angle*Math.PI/180.0
        quaternion.setRoll(rotation[0])
    }
    // ~ degrees
    fun setPitch(angle:Double) {
        rotation[1] = angle*Math.PI/180.0
        quaternion.setPitch(rotation[1])
    }
    // ~ degrees
    fun setYaw(angle:Double) {
        rotation[2] = angle*Math.PI/180.0
        quaternion.setYaw(rotation[2])
    }

    // Roll, pitch, yaw are in degrees. Convert to radians.
    // This refers to the orientation of the link with respect to the
    // previous link.
    fun setRpy(roll:Double,pitch:Double,yaw:Double) {
        setRoll(roll)
        setPitch(pitch)
        setYaw(yaw)
        quaternion.update()
    }

    /** Recalculate quaternion. No need unless angle has changed.
     * Note. joint coordinates are with respect robot reference frame
     *       when straight. "z" is up, "x" is front, "y" is across.
     *
     * In our robot, adjacent motors (joints) are either aligned with parallel
     * axes or at right angles to each other as defined by the "rotation" angle.
     * Refer to "How to Calculate a Robot's Forward Kinematics in 5 Easy Steps"
     * by Alex Owen-Hill, these are the equivalents to our coordinate matrix:
     *
     * Distances _mm, angle in radians
     */
    fun update() {
        // No work if we haven't moved pitch since last update
        // (pitch is the only "live" angle)
        if(sourcePin.angle==currentAngle) return
        currentAngle = sourcePin.angle

        quaternion.setRoll(rotation[0])
        quaternion.setPitch(sourcePin.angle+rotation[1]-sourcePin.home)
        quaternion.setYaw(rotation[2])

        quaternion.update()

        if(DEBUG) LOGGER.info(String.format("%s.update: %s %2.2f,%2.2f,%2.2f",CLSS,name,
                rotation[0]*180.0/Math.PI,(sourcePin.angle+rotation[1])*180.0/Math.PI,rotation[2]*180.0/Math.PI))
    }

    private val CLSS = "Link"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean
    /**
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        currentAngle = Double.NaN
        rotation = doubleArrayOf(0.0,0.0,0.0)
        quaternion = Quaternion()
        endPin = LinkPin(PinType.ORIGIN)     // Must be configured
        sourcePin = LinkPin(PinType.ORIGIN)  // Origin until set otherwise.
        side = Side.FRONT
    }
}