/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.util

import android.graphics.PointF

/**
 * This class is a holder for a list of strings.
 * Its purpose is to make it easy to format JSON.
 * nam - the name of the list
 * jtype  - the name as a 4 character key.
 */
class NamedPointList (val nam:String, val jtype:String) {
    val name = nam
    val key  = jtype
    val list = mutableListOf<PointF>()

    fun add(value:PointF) {
        list.add(value)
    }
}