/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

/**
 * Position of a joint or extremity in space
 */
data class ExtremityLocation(val extremity: Extremity, val pos: Point3D) {
}
