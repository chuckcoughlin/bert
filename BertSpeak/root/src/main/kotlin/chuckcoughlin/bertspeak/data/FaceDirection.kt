/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

/**
 * This class is a holder for parameters that define
 * the direction of a face in terms of sines of 2 angles.
 * Its purpose is to make it easy to format JSON.
 * x - euler angle in y-z plane.
 * y - euler angle in x-z
 * z - euler angle in x-y plane (view from side-to-side)
 */
class FaceDirection () {
    var x = 0.0f
    var y = 0.0f
    var z = 0.0f
}
