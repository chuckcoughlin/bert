/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bertspeak.common.ConfigurationConstants
import chuckcoughlin.bertspeak.service.DispatchService.Companion.CLSS
import chuckcoughlin.bertspeak.ui.graphics.Side
import java.util.logging.Logger

/**
 * Current position and orientation of a joint or end-effector in 3 space.
 * with respect to the root of the robot inertial coordinate system.
 * Each JointTree has a complete and separate set of these objects.
 */
class JointPosition() {
	var joint: Joint
	var orientation: DoubleArray // Angles with respect to system normal
	var pos: Point3D   // Coordinates of joint or end effector

	fun positionToText() : String {
		return(String.format("%2.1f,%2.1f,%2.1f",pos.x,pos.y,pos.z))
	}
	fun orientationToText() : String {
		return(String.format("%2.0f,%2.0f,%2.0f",orientation[0],orientation[1],orientation[2]))
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

	companion object {
		val NONE = JointPosition()
	}

	init {
		joint = Joint.NONE
		orientation = doubleArrayOf(0.0,0.0,0.0)
		pos    = Point3D(0.0,0.0,0.0)
	}
}
