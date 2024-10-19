/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * This class is a holder for parameters that define
 * the direction to a face in terms of sines of 2 angles.
 * Its purpose is to make it easy to format JSON.
 * elevation - angle fron x-y plane to the face
 * azimuth - angle in x-y plane to the face
 */
class FaceDirection () {
    var elevation = 0.0f
    var azimuth = 0.0f
}
