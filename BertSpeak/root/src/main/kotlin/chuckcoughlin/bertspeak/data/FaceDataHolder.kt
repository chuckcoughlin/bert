/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.util

import android.graphics.PointF

/**
 * This class is a holder for parameters that define
 * a face.
 * Its purpose is to make it easy to format JSON.
 * nam - the name of the list
 * jtype  - the name as a 4 character key.
 */
class FaceDataHolder () {
    val landmarks = mutableListOf<NamedPoint>()

    fun addLandmark(p:NamedPoint) {
        landmarks.add(p)
    }
}