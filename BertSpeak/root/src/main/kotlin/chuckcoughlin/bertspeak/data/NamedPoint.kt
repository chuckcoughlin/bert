/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.util

import android.graphics.PointF

/**
 * This class is a holder for a facial landmark.
 * It contains a name and a 2D float point.
 */
class NamedPoint (val nam:String, x1:Float,x2:Float) {
    val name = nam
    val point = Point2D(x1,x2)
}