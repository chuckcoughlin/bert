/**
 * Copyright 2023-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import java.util.logging.Logger

/**
 * A joint-link is a skeletal structure beginning with a source joint and ending
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
class JointLink( val source:Joint,val end:Joint ) {
    // Values are degrees.
    var transform: Quaternion
    private var orientation:DoubleArray
    private var coordinates:DoubleArray
    var home:Double
    val endJoint    = end
    val sourceJoint = source
    var side:Side

    fun applyTransform(q:Quaternion) : Quaternion {
        val end = q.postMultiplyBy(transform)
        return end
    }
    // These are the physical fixed distances between source
    // and end joint from the URDF file. The joint angle
    // is in its home position. Initialize position to same.
    // ~mm
    fun setCoordinates(x:Double,y:Double,z:Double) {
        if(DEBUG) LOGGER.info(String.format("%s.setCoordinates: (%s) %2.2f,%2.2f,%2.2f",CLSS,endJoint.name,x,y,z))
        coordinates[0] = x
        coordinates[1] = y
        coordinates[2] = z
    }

    /*
     * Compute the transform quaternion once the corrdinates
     * and orientation are set.
     */
    fun update(theta:Double) {
        transform.setRoll(orientation[0])
        transform.setPitch(orientation[1])
        transform.setYaw(orientation[2])
        transform.setTranslation(coordinates[0],coordinates[1],coordinates[2])
        transform.update()
    }

    // Roll, pitch, yaw are in degrees. Convert to radians.
    // This refers to the orientation of the origin
    // with respect to the previous link. ~ degrees
    fun setRpy(roll:Double,pitch:Double,yaw:Double) {
        orientation[0] = roll
        orientation[1] = pitch
        orientation[2] = yaw
    }

    fun clone() : JointLink {
        val copy = JointLink(sourceJoint,endJoint)
        copy.coordinates = coordinates.clone()
        copy.orientation = orientation.clone()
        copy.transform    = transform.clone()
        copy.home = home
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
        coordinates = doubleArrayOf(0.0,0.0,0.0)  // end referenced to source
        orientation = doubleArrayOf(0.0,0.0,0.0)
        transform    = Quaternion()
        home = 0.0
        side = Side.FRONT
    }
}