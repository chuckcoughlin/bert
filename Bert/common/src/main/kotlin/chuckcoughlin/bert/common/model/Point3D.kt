/**
 * Copyright 2024-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * Use our own class of Point to make it easier to serialize and deserialize.
 */
data class Point3D (var x:Double, var y:Double,var z:Double) {
	fun toText():String {
		return String.format("%3.2f,%3.2f,%3.2f",x,y,z)
	}
}