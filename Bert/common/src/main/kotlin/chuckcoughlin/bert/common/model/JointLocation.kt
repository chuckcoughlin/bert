/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * Position of a joint in space
 */
data class JointLocation(val joint:Joint,val pos:Point3D) : Location {
	override val name = joint.name
	override val point = pos
	override val isJoint = true
}