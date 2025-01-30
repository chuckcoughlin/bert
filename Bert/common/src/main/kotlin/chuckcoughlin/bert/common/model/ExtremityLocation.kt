/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * Position of an extremity in space
 */
data class ExtremityLocation(val extremity:Extremity,val pos:Point3D) : Location {
	override val name = extremity.name
	override val point = pos
	override val isJoint = false
}