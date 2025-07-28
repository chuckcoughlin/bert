/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RectShape
import chuckcoughlin.bertspeak.data.LinkLocation
import chuckcoughlin.bertspeak.data.Point2D

/**
 * Draw a filled rectangle with circles at either end
 * the indicate the joint.
 */
class BoneDrawable(loc:LinkLocation) : LinkShapeDrawable(loc) {

	//
	override fun draw(canvas: Canvas,gc:GraphicsConfiguration) {
		val radius = 10f*gc.scale.toFloat()  // 10 mm to Scale
		val w = 5f*gc.scale.toFloat()   // 10 mm to Scale, bone width
		canvas.drawCircle((loc.source.x.toFloat()),loc.source.y.toFloat(),radius,gc.foreground)
		canvas.drawCircle((loc.end.x.toFloat()),loc.end.y.toFloat(),radius,gc.foreground)
		canvas.drawRect(loc.source.x.toFloat()-w,loc.source.y.toFloat()-w,
			           loc.end.x.toFloat()+w,loc.end.y.toFloat()+w,gc.foreground) // left,top,right,bottom
	}

	init {
	}
}
