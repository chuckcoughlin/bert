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
 * Coordinates of a joint or end-effector in space.
 */
class JointPosition() {
	var joint: Joint
	var parent: JointPosition    // Hashcode of the parent link
	var pos: Point3D   // Coordinates of joint or end effector
	var side: String   // Link group

	fun positionToText() : String {
		return String.format("%s coordinates: %s->%s [%s,%s,%s]",pos.toText(),joint.name,if(Joint.isEndEffector(joint)) "(end effector)" else "",side)
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
		pos    = Point3D(0.0,0.0,0.0)
		side = Side.FRONT.name
	}
}
