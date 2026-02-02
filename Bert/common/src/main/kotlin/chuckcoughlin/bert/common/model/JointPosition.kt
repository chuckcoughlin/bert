/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.math.Rom

/**
 * Current position and orientation of a joint or end-effector in 3 space.
 * with respect to the root of the robot inertial coordinate system.
 * Each JointTree has a complete and separate set of these objects.
 */
class JointPosition() {
	var joint: Joint
	var parent: Joint    //
	var orientation: DoubleArray // Angles with respect to system normal
	var pos: Point3D   // Coordinates of joint or end effector
	var side: String   // Link group

	fun positionToText() : String {
		return String.format("%s%s coordinates: [%s]",joint.name,if(Joint.isEndEffector(joint)) "(end effector)" else "",pos.toText(),)
	}

	/**
	 * Create a quaternion that handles rotation only, no translation
	 */
	fun quaternionToRotate(): Quaternion {
		val rom = Rom()
		rom.setRoll(orientation[0])
		rom.setPitch(orientation[1])
		rom.setYaw(orientation[2])
		return Quaternion.quaternionFromRotationMatrix(rom)
	}
	fun setJointAngle(theta:Double) {
		orientation[1] = theta
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
		copy.parent = parent
		copy.pos = pos.copy()
		copy.side = side
		return copy
	}

	fun updateFromQuaternion(q:Quaternion) {
		orientation[0] = q.direction()[0]
		orientation[1] = q.direction()[1]
		orientation[3] = q.direction()[2]
		pos.x = q.position().x
		pos.y = q.position().y
		pos.z = q.position().z
	}

	companion object {
		val NONE = JointPosition()
	}

	init {
		joint = Joint.NONE
		parent = Joint.NONE
		orientation = doubleArrayOf(0.0,0.0,0.0)
		pos    = Point3D(0.0,0.0,0.0)
		side = Side.FRONT.name
	}
}
