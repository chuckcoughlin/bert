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
class AppendageDrawable(name:String,val p1:Point2D, p2: Point2D, side:Side) : LinkShapeDrawable(name,p2,side) {
	val unscaledRadius:Float

	// Error indicator is a red circle
	override fun draw(canvas: Canvas,gc:GraphicsConfiguration) {
		val radius = gc.scale*unscaledRadius
		val x = gc.originx + p2.x.toFloat()*gc.scale
		val y = gc.originy + p2.y.toFloat()*gc.scale
		canvas.drawCircle(x,y,radius,if(selected) gc.selectedColor else gc.topColor)
	}

	init {
		unscaledRadius = 10f
 	}
}
