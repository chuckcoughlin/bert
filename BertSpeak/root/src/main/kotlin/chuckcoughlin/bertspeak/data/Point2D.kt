/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.util

/**
 * Use our own class of Point to make it easier to serialize and deserialize.
 */
data class Point2D (val x1:Float, val x2:Float) {
    val x: Float = x1
    val y: Float = x2
}