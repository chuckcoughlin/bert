/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RectShape
import chuckcoughlin.bertspeak.data.Point2D

/**
 * Draw a filled rectangle with circles at either end
 * the indicate the joint.
 */
class BoneDrawable(val source: Point2D,val end:Point2D, val scale:Double) : LinkShapeDrawable() {
	val radius: Float
	val w:Float
	val black: Paint
	val gray: Paint
	//
	override fun draw(canvas: Canvas) {
		canvas.drawCircle((source.x.toFloat()),source.y.toFloat(),radius,black)
		canvas.drawCircle((end.x.toFloat()),end.y.toFloat(),radius,black)
		canvas.drawRect(source.x.toFloat()-w,source.y.toFloat()-w,
			           end.x.toFloat()+w,end.y.toFloat()+w,gray) // left,top,right,bottom
	}

	init {
		black = Paint().apply { setARGB(255,0,0,0) }
		gray = Paint().apply { setARGB(255,200,200,200) }
		radius = 10f*scale.toFloat()  // 10 mm to Scale
		w = 5f*scale.toFloat()   // 10 mm to Scale, bone width
	}
}