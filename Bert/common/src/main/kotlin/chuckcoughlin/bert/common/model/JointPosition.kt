/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion

/**
 * Current position and orientation of a joint or end-effector in 3 space.
 * with respect to the root of the robot inertial coordinate system.
 * Each JointTree has a complete and separate set of these objects.
 * All angles in degrees
 */
class JointPosition() {
	var joint: Joint
	var orientation: DoubleArray // Angles with respect to system normal
	var pos: Point3D             // Coordinates of joint or end effector
	var theta: Double


	fun positionToText() : String {
		return(String.format("%3.1f,%3.1f,%3.1f",pos.x,pos.y,pos.z))
	}
	fun orientationToText() : String {
		return(String.format("%3.0f,%3.0f,%3.0f",orientation[0],orientation[1],orientation[2]))
	}
	fun setJointAngle(angle:Double) {
		theta = angle
	}

	fun setOrientation(phi:Double,theta:Double,psi:Double) {
		orientation = doubleArrayOf(phi,theta,psi)
	}

	fun setPosition(x:Double,y:Double,z:Double) {
		pos.x = x
		pos.y = y
		pos.z = z
	}

	fun copy() : JointPosition {
		val copy = JointPosition()
		copy.joint = joint
		copy.pos = pos.copy()
		return copy
	}

	fun updateFromQuaternion(q:Quaternion) {
		orientation[0] = q.direction()[0] * 180.0/Math.PI
		orientation[1] = q.direction()[1] * 180.0/Math.PI
		orientation[2] = q.direction()[2]*  180.0/Math.PI
		pos.x = q.position().x
		pos.y = q.position().y
		pos.z = q.position().z
	}

	companion object {
		val NONE = JointPosition()
	}

	init {
		joint = Joint.NONE
		orientation = doubleArrayOf(0.0,0.0,0.0)
		pos    = Point3D(0.0,0.0,0.0)
		theta = 0.0
	}
}
