/**
 * Copyright 2023-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import java.util.logging.Logger

/**
 * A joint-link is a skeletal structure connecting a source joint to either a
 * revolute joint or end effector. The "coordinates" and "rotation" members are
 * static and refer to the orientation and position of the end joint with respect
 * to the source joint.
 *
 * If the end represents a joint, then the joint is a "revolute", that is
 * rotational only. The only translations are the fixed distances between source
 * and end joints. The joint x-axis is always at right angles to a line from the
 * source to end.
 *
 * The end "joint" may also represent an "extremity" or "end effector". We call it an
 * "appendage". An appendage is not movable, but we can calculate its 3D location
 * and orientation.
 *
 * Multiple joint-links may have the same source, indicating they are on the same physical
 * skeletal piece.
 *
 * @param name link name - either the appendage or joint name
 */
class JointLink( val end:JointPosition,val source:JointPosition ) {
    var quaternion: Quaternion   // Associated with the source joint
    // Current angle was in effect last time Q evaluated.
    // Values are degrees.
    private var currentPitch: Double  // Motor angle
    private var rotation:DoubleArray
    val name = end.name
    val endJoint   = end
    val sourceJoint= source
    var side:Side

    // ~mm
    fun setCoordinates(x:Double,y:Double,z:Double) {
        if(DEBUG) LOGGER.info(String.format("%s.setCoordinates: (%s) %2.2f,%2.2f,%2.2f",CLSS,name,x,y,z))
        quaternion.setTranslation(x,y,z)
        quaternion.update()
    }

    /**
     * Update quaternion to reflect a change in the motor angle
     */
    fun updateForMotorAngle(angle:Double) {
        setPitch(angle)

    }
    // ~ degrees
    fun setRoll(angle:Double) {
        rotation[0] = angle*Math.PI/180.0
    }
    // ~ degrees
    fun setPitch(angle:Double) {
        rotation[1] = angle*Math.PI/180.0
    }
    // ~ degrees
    fun setYaw(angle:Double) {
        rotation[2] = angle*Math.PI/180.0
    }

    // Roll, pitch, yaw are in degrees. Convert to radians.
    // This refers to the orientation of the link with respect to the
    // previous link.
    fun setRpy(roll:Double,pitch:Double,yaw:Double) {
        setRoll(roll)
        setPitch(pitch)
        setYaw(yaw)
        recalculate()
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
    fun recalculate() {
        // No work if we haven't moved pitch since last update
        // (pitch is the only "live" angle)
        if(currentPitch==rotation[1]) return   // Nothing to do
        currentPitch = rotation[1]

        quaternion.setRoll(rotation[0])
        quaternion.setPitch(currentPitch+rotation[1]-source.home)
        quaternion.setYaw(rotation[2])
        quaternion.update()

        if(DEBUG) LOGGER.info(String.format("%s.update: %s %2.2f,%2.2f,%2.2f",CLSS,name,
                rotation[0]*180.0/Math.PI,(currentPitch+rotation[1])*180.0/Math.PI,rotation[2]*180.0/Math.PI))
    }

    fun clone() : JointLink {
        val copy = JointLink(endJoint.copy(),sourceJoint.copy())
        copy.currentPitch = currentPitch
        copy.rotation = rotation.clone()
        copy.quaternion = quaternion.clone()
        copy.side = side
        return copy

    }

    private val CLSS = "JointLink"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean
    /**
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        currentPitch = Double.NaN
        rotation = doubleArrayOf(0.0,0.0,0.0)
        quaternion = Quaternion()
        side = Side.FRONT
    }
}