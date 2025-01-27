/**
 * Copyright 2024-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

/**
 * Use our own class of Point to make it easier to serialize and deserialize.
 */
data class Point2D (val x1:Double, val x2:Double) {
    val x: Double = x1
    val y: Double = x2
}
