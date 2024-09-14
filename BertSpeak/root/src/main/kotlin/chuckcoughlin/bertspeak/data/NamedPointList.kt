/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

/**
 * This class is a holder for a list of strings.
 * Its purpose is to make it easy to format JSON.
 * nam - the name of the list
 */
class NamedPointList (val nam:String) {
    val name = nam
    val list = mutableListOf<Point2D>()

    fun add(x1:Float,x2:Float) {
        list.add(Point2D(x1,x2))
    }
}
