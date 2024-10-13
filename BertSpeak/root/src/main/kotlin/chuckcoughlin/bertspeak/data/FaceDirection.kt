/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

/**
 * This class is a holder for parameters that define
 * the direction of a face in terms of sines of 2 angles.
 * Its purpose is to make it easy to format JSON.
 * phi - angle in x-y plane. 0 corresponds to Y axis
 * theta - angle from x-y plane. theta = 90 is the z-axis
 */
class FaceDirection () {
    var phi = 0.0f
    var theta = 0.0f
}
