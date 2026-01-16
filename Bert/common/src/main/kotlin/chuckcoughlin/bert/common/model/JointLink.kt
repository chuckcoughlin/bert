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
class JointLink( val source:JointPosition,val end:JointPosition ) {
    var quaternion: Quaternion   // Associated with the source joint
    // Values are degrees.
    private var orientation:DoubleArray
    private var rotation:DoubleArray
    val name = end.name
    val endJoint   = end
    val sourceJoint= source
    var side:Side



    // These are the physical fixed distances between source
    // and end joint.
    // ~mm
    fun setDimensions(x:Double,y:Double,z:Double) {
        if(DEBUG) LOGGER.info(String.format("%s.setCoordinates: (%s) %2.2f,%2.2f,%2.2f",CLSS,name,x,y,z))
        quaternion.setTranslation(x,y,z)
    }
    // Roll, pitch, yaw are in degrees. Convert to radians.
    // This refers to the orientation of the origin
    // with respect to the previous link. ~ degrees
    fun setRpy(roll:Double,pitch:Double,yaw:Double) {
        orientation[0] = roll
        orientation[1] = pitch
        orientation[2] = yaw
    }

    /**
     * Update quaternion to reflect a change in the source motor angle
     */
    fun updateForMotorAngle(angle:Double) {
        if(DEBUG) LOGGER.info(String.format("%s.updateForMotorAngle: %s = %2.2f",CLSS,name,angle))
        setPitch(angle)

    }
    // ~ degrees
    fun setRoll(angle:Double) {
        rotation[0] = angle
    }
    // ~ degrees
    fun setPitch(angle:Double) {
        rotation[1] = angle
    }
    // ~ degrees
    fun setYaw(angle:Double) {
        rotation[2] = angle
    }

    /** Update the quaternion to reflect current angle settings.
     * Note. joint coordinates are with respect robot reference frame
     *       when straight. "z" is up, "x" is to front, "y" is across.
     *
     * In our robot, adjacent motors (joints) are either aligned with parallel
     * axes or at right angles to each other as defined by the "rotation" angle.
     * Refer to "How to Calculate a Robot's Forward Kinematics in 5 Easy Steps"
     * by Alex Owen-Hill, these are the equivalents to our coordinate matrix:
     *
     * Distances _mm, angle in degrees
     */
    fun recalculate() {
        // pitch is the only "live" angle
        quaternion.setRoll((rotation[0]+orientation[0])*Math.PI/180.0)
        quaternion.setPitch((rotation[1]+orientation[1])*Math.PI/180.0)
        quaternion.setYaw((rotation[2]+orientation[2])*Math.PI/180.0)
        quaternion.update()

        if(DEBUG) LOGGER.info(String.format("%s.recalculate: %s %2.2f,%2.2f,%2.2f",CLSS,name,
                rotation[0]+orientation[0],rotation[1]+orientation[1],rotation[2]+orientation[2]))
    }

    /**
     * Rotate the link around the source joint to the specified orientation. This movement is only
     * appropriate for the IMU as all other joints are attached to the
     * preceding link.
     * @Return the resulting quaternion
     */
    fun rotate() {
        quaternion.setRoll((orientation[0])*Math.PI/180.0)
        quaternion.setPitch((orientation[1])*Math.PI/180.0)
        quaternion.setYaw((orientation[2])*Math.PI/180.0)
        quaternion.rotate()
    }

    fun clone() : JointLink {
        val copy = JointLink(sourceJoint.copy(),endJoint.copy())
        copy.rotation = rotation.clone()
        copy.orientation = orientation.clone()
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
        orientation = doubleArrayOf(0.0,0.0,0.0)
        rotation    = doubleArrayOf(0.0,0.0,0.0)
        quaternion = Quaternion()
        side = Side.FRONT
    }
}