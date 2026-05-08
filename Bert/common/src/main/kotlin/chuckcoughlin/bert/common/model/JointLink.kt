/**
 * Copyright 2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * A link is a skeletal structure beginning with a source joint and ending
 * with either another joint or end effector. Values are static (except for theta).
 * The joint is modelled as a revolute joint, rotating on the source y-axis. The
 * "orientation" refers to the fixed orientation of tne source joint to the
 * previous limb when the joint angle is at zero with respect to the previous link.
 * The joint y-axis value is modified by "theta" the current joint angle. The
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
class JointLink(source:Joint,end:Joint)  {
    // Values are degrees.
    var orientation:DoubleArray
    var coordinates:DoubleArray
    val endJoint:Joint
    val sourceJoint:Joint
    var home:Double
    var side:String
    var theta:Double

    // These are the physical fixed distances between source
    // and end joint from the URDF file. The joint angle
    // is in its reference home position.
    // ~mm
    fun setCoordinates(x:Double,y:Double,z:Double) {
        coordinates[0] = x
        coordinates[1] = y
        coordinates[2] = z
    }

    fun updateJointAngle(angle:Double) {
        theta = angle
    }

    // Roll, pitch, yaw are in degrees.
    // This refers to the orientation of the origin
    // with respect to the previous link. ~ degrees
    fun setRpy(roll:Double,pitch:Double,yaw:Double) {
        orientation[0] = roll
        orientation[1] = pitch
        orientation[2] = yaw
    }

    fun clone() : JointLink {
        val copy = JointLink(sourceJoint,endJoint)
        //copy.transform    = transform.clone()
        copy.home = home
        copy.side = side
        copy.setCoordinates(coordinates[0],coordinates[1],coordinates[2])
        copy.setRpy(orientation[0],orientation[1],orientation[2])
        copy.theta = theta
        return copy
    }
    init {
        sourceJoint = source
        endJoint    = end
        coordinates = doubleArrayOf(0.0,0.0,0.0)  // end referenced to source
        orientation = doubleArrayOf(0.0,0.0,0.0)
        home = 0.0
        side = Side.FRONT.name
        theta = 0.0
    }
}