/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data


/**
 * This class is a holder for parameters that define
 * a face. The name of the owner of the face is separate.
 * Its purpose is to make it easy to format JSON.
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
