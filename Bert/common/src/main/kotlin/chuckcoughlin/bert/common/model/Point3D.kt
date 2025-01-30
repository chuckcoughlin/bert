/**
 * Copyright 2024-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * Use our own class of Point to make it easier to serialize and deserialize.
 */
data class Point3D (val x:Double, val y:Double,val z:Double) {
}