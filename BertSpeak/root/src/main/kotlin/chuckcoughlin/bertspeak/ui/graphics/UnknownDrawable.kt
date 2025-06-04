/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Canvas
import android.graphics.Paint
import chuckcoughlin.bertspeak.data.Point2D

/**
 * Draw a red dot if nothing else matches..
 */
class UnknownDrawable(val pos: Point2D,val scale:Double) : LinkShapeDrawable() {
	val radius: Float
	val red: Paint

	// Error indicator is a red circle
	override fun draw(canvas: Canvas) {
		canvas.drawCircle((pos.x.toFloat()),pos.y.toFloat(),radius,red)
	}

	init {
		red = Paint().apply { setARGB(255,255,0,0) }
		radius = 10f*scale.toFloat()  // 10 mm to Scale
 	}
}