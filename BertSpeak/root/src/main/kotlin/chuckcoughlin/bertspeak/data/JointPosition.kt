/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bertspeak.common.ConfigurationConstants
import chuckcoughlin.bertspeak.ui.graphics.Side

/**
 * Coordinates of a joint or end-effector in space..
 */
class JointPosition() {
	var id: Int
	var name: String
	var parent: Int    // Hashcode of the parent link
	var pos: Point3D   // Coordinates of joint or end effector
	var isAppendage:Boolean
	var side: String   // Link group

	fun positionToText() : String {
		return String.format("%s coordinates: %s->%s [%s,%s,%s]",pos.toText(),name,if(isAppendage) "(appendage)" else "",side)
	}

	fun copy() : JointPosition {
		val copy = JointPosition()
		copy.id = id
		copy.name = name
		copy.parent = parent
		copy.pos = pos.copy()
		copy.isAppendage = isAppendage
		copy.side = side
		return copy
	}

	init {
		id = hashCode()
		name = ConfigurationConstants.NO_NAME
		parent = ConfigurationConstants.NO_ID
		pos    = Point3D(0.0,0.0,0.0)
		isAppendage = false
		side = Side.FRONT.name
	}
}
