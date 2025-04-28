/**
 * Copyright 2023-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Axis
import chuckcoughlin.bert.common.math.Quaternion
import java.util.logging.Logger

/**
 * A link is a skeletal structure connecting a source pin to either a
 * revolute or end effector. The "coordinates" and "rotation" members are
 * static and refer to the orientation and position of the end pin with respect
 * to the source pin. The position of the link is the position of the
 * end point. It is a dynamic quantity derived from the quaternion matrix.
 * It represents distances and orientation with respect to the robot
 * inertial frame.
 *
 * If the end pin represents a joint, then the pin is a "revolute", that is
 * rotational only. There is no translation beyond the dimensions of the link.
 * The joint x-axis is always at right angles to a line from the source to end.
 *
 * The end pin may also represent an "extremity" or "end effector". We call it an
 * "appendage". An appendage is not movable, but we can calculate its 3D location
 * and orientation.
 *
 * Multiple links may have the same source, indicating they are on the same physical
 * skeletal piece.
 *
 * @param name link name
 */
class Link( val name:String ) {
    val quaternion: Quaternion
    // Current angle was in effect last time Q evaluated.
    // Values are degrees.
    private var currentAngle: Double  // Motor angle
    private var coordinates: Point3D
    private var rotation:DoubleArray
    var endPin:    LinkPin
    var sourcePin: LinkPin

    // ~mm
    fun setCoordinates(x:Double,y:Double,z:Double) {
        if(DEBUG) LOGGER.info(String.format("%s.setCoordinates: (%s) %2.2f,%2.2f,%2.2f",CLSS,name,x,y,z))
        coordinates = Point3D(x,y,z)
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
        // No work if we haven't moved since last update
        if(sourcePin.angle==currentAngle) return

        currentAngle = sourcePin.angle

        // **** Different combinations of motor axis alignments ****
        when (sourcePin.axis) {
            Axis.X -> {
                quaternion.setRoll(sourcePin.angle+rotation[0])
            }
            Axis.Y -> {
                quaternion.setPitch(sourcePin.angle+rotation[1])
            }
            Axis.Z -> {
                quaternion.setYaw(sourcePin.angle+rotation[2])
            }
        }
        quaternion.update()

        if(DEBUG) LOGGER.info(String.format("%s.update: %s (%s) %2.2f,%2.2f,%2.2f",CLSS,name,
                sourcePin.axis.name,rotation[0]*180.0/Math.PI,rotation[1]*180.0/Math.PI,
                                    rotation[2]*180.0/Math.PI))
    }

    private val CLSS = "Link"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean
    /**
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        currentAngle = Double.NaN
        coordinates = Point3D(0.0,0.0,0.0)
        rotation = doubleArrayOf(0.0,0.0,0.0)
        quaternion = Quaternion()
        endPin = LinkPin(PinType.ORIGIN)     // Must be configured
        sourcePin = LinkPin(PinType.ORIGIN)  // Origin until set otherwise.
    }
}