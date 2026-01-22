/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Canvas
import android.graphics.Paint
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bertspeak.data.Point2D

/**
 * Draw a red dot if nothing else matches..
 */
class UnknownDrawable(joint: Joint, p2: Point2D, side:Side) : LinkShapeDrawable(joint,p2,side) {
	val unscaledRadius:Float

	// Error indicator is a red circle
	override fun draw(canvas: Canvas,gc:GraphicsConfiguration) {
		val radius = gc.scale*unscaledRadius
		val x = gc.originx + p2.x.toFloat()*gc.scale
		val y = gc.originy + p2.y.toFloat()*gc.scale
		canvas.drawCircle(x,y,radius,gc.errorColor)
	}

	init {
		unscaledRadius = 10f
 	}
}
