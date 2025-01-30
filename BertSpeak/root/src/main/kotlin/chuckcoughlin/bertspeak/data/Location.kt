/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bertspeak.data.Point3D

/**
 * Position of a joint or extremity in space
 */
interface Location {
	val isJoint:Boolean
	val name:String
	val point: Point3D
}