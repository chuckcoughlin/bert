/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Half.toFloat
import chuckcoughlin.bertspeak.data.LinkLocation
import chuckcoughlin.bertspeak.data.Point2D

/**
 * Draw a red dot if nothing else matches..
 */
class UnknownDrawable(loc: LinkLocation) : LinkShapeDrawable(loc) {
	val red: Paint

	// Error indicator is a red circle
	override fun draw(canvas: Canvas,gc:GraphicsConfiguration) {
		val radius = (gc.scale*10f).toFloat()
		canvas.drawCircle((loc.source.x.toFloat()),loc.source.y.toFloat(),radius,red)
	}

	init {
		red = Paint().apply { setARGB(255,255,0,0) }
 	}
}
