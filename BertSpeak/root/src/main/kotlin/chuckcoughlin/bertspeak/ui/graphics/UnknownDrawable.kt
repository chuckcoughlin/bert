/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Canvas
import android.graphics.Paint
import chuckcoughlin.bertspeak.data.LinkLocation

/**
 * Draw a red dot if nothing else matches..
 */
class UnknownDrawable(loc: LinkLocation) : LinkShapeDrawable(loc) {
	val unscaledRadius:Float
	val redPaint: Paint

	// Error indicator is a red circle
	override fun draw(canvas: Canvas,gc:GraphicsConfiguration) {
		val radius = gc.scale*unscaledRadius
		val x = gc.originx + loc.source.x.toFloat()*gc.scale
		val y = gc.originy + loc.source.y.toFloat()*gc.scale
		canvas.drawCircle(x,y,radius,redPaint)
	}

	init {
		redPaint = Paint().apply { setARGB(255,255,0,0) }
		unscaledRadius = 10f
 	}
}
