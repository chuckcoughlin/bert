/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * This class is a holder for parameters that define
 * a face.
 * Its purpose is to make it easy to format JSON.
 * nam - the name of the list
 * jtype  - the name as a 4 character key.
 */
class FacialDetails () {
    val contours  = mutableMapOf<String,MutableMap<Int,Point2D>>()
    val landmarks = mutableMapOf<String,Point2D>()

    fun addContourPoint(name:String,index:Int,p: Point2D) {
        if( contours[name]==null ) contours[name] = mutableMapOf<Int,Point2D>()
        val contour = contours[name]
        contour!!.set(index,p)
    }
    fun addLandmark(name:String,p: Point2D) {
        landmarks.set(name,p)
    }
}