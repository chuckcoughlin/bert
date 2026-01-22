/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * Current position and orientation of a joint or end-effector in 3 space.
 * Orientation is with respect to the robot inertial coordinate system.
 * Each JointTree has a complete and separate set of these objects.
 */
class JointPosition() {
	var joint: Joint
	var parent: JointPosition    // Hashcode of the parent link
	var orientation: DoubleArray // Angles with respect to system normal
	var pos: Point3D   // Coordinates of joint or end effector
	var side: String   // Link group

	fun positionToText() : String {
		return String.format("%s coordinates: %s->%s [%s,%s,%s]",pos.toText(),joint.name,if(Joint.isEndEffector(joint)) "(end effector)" else "",side)
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

	companion object {
		val NONE = JointPosition()
	}

	init {
		joint = Joint.NONE
		parent = JointPosition()
		orientation = doubleArrayOf(0.0,0.0,0.0)
		pos    = Point3D(0.0,0.0,0.0)
		side = Side.FRONT.name
	}
}
