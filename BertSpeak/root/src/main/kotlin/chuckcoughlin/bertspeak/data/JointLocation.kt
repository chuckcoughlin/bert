/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

/**
 * Position of a joint or extremity in space
 */
data class JointLocation(val joint:Joint,val pos:Point3D) : Location {
	override val name = joint.name
	override val point = pos
	override val isJoint = true
}
