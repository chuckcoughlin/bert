/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * Position of an appendage (end effectoappendager) in space
 */
data class AppendageLocation(val appendage:Appendage,val pos:Point3D) : Location {
	override val name = appendage.name
	override val point = pos
	override val isJoint = false
}