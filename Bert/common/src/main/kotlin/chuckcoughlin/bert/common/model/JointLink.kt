/**
 * Copyright 2023-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import java.util.logging.Logger

/**
 * A joint-link is a skeletal structure beginning with a source joint and ending
 * with either another joint or end effector. Values are static with exception of
 * the joint angle. The "orientation" refers to the
 * fixed orientation of tne source joint with respect to the previous link. Thw
 * "coordinates" refer to the location of the end joint or end-effector with
 * respect to the source. The axes for these coordinates are always "y" corresponding
 * to the axis of the joint, "x" is positive forward, "z" is up.
 *
 * Multiple joint-links may have the same source, indicating they are on the same physical
 * skeletal piece. A link may be uniquely identified by its end joint/effector.
 *
 * @param source position of the source joint
 * @param end position of the end joint or end effector
 */
class JointLink( val source:Joint,val end:Joint ) {
    // Values are degrees.
    private var orientation:DoubleArray
    private var position:DoubleArray
    private var coordinates:DoubleArray
    var home:Double
    val endJoint    = end
    val sourceJoint = source
    var side:Side



    // These are the physical fixed distances between source
    // and end joint from the URDF file. The joint angle
    // is in its home position. Initialize position to same.
    // ~mm
    fun setCoordinates(x:Double,y:Double,z:Double) {
        if(DEBUG) LOGGER.info(String.format("%s.setCoordinates: (%s) %2.2f,%2.2f,%2.2f",CLSS,endJoint.name,x,y,z))
        coordinates[0] = x
        coordinates[1] = y
        coordinates[2] = z

        position[0] = x
        position[1] = y
        position[2] = z
    }

    /*
     * Modify the position, accounting for the joint angle.
     */
    fun setJointAngle(theta:Double) {
        val angle = (theta-home)*Math.PI/180.0
        if(DEBUG) LOGGER.info(String.format("%s.setJointAngle:(%s) %2.2f",CLSS,endJoint.name,theta))
        coordinates[0] = coordinates[0]*Math.cos(angle)+ coordinates[2]*Math.sin(angle)
        position[1]    = coordinates[1]
        position[2]    = coordinates[0]*Math.sin(angle)+ coordinates[2]*Math.cos(angle)
    }
    // Roll, pitch, yaw are in degrees. Convert to radians.
    // This refers to the orientation of the origin
    // with respect to the previous link. ~ degrees
    fun setRpy(roll:Double,pitch:Double,yaw:Double) {
        orientation[0] = roll
        orientation[1] = pitch
        orientation[2] = yaw
    }
    fun setEndPosition(jp1:JointPosition,jp2:JointPosition) {
        val q = Quaternion.quaternionForJointPosition(jp1)

        //q.rotate()
        //q.translate()
        //q.updateJointPosition(jp2)
    }

    fun clone() : JointLink {
        val copy = JointLink(sourceJoint,endJoint)
        copy.coordinates = coordinates.clone()
        copy.orientation = orientation.clone()
        copy.position    = position.clone()
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
        position    = doubleArrayOf(0.0,0.0,0.0)  // end accounting for angle
        home = 0.0
        side = Side.FRONT
    }
}