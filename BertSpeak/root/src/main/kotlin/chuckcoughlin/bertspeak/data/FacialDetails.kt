/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data


/**
 * This class is a holder for parameters that define
 * a face.
 * Its purpose is to make it easy to format JSON.
 * nam - the name of the list
 * jtype  - the name as a 4 character key.
 */
class FacialDetails () {
    val contours  = mutableMapOf<String,MutableList<Point2D>>()
    val landmarks = mutableListOf<NamedPoint>()

    /**
     * The name in the NamedPoint is the contour name
     */
    fun addContourPoint(name:String,p: Point2D) {
        if( contours[name]==null ) contours[name] = mutableListOf<Point2D>()
        val contour = contours[name]
        contour!!.add(p)
    }
    fun addLandmark(p: NamedPoint) {
        landmarks.add(p)
    }
}
