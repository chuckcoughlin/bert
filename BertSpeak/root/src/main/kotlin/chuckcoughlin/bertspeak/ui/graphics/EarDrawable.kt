/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Canvas
import android.graphics.Paint
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bertspeak.data.Point2D
import chuckcoughlin.bertspeak.data.Side

/**
 * Draw a black circle for an ear.
 */
class EarDrawable(joint: Joint, val p1:Point2D, p2: Point2D, side: Side) : LinkShapeDrawable(joint,p2,side) {
	val unscaledRadius:Float
	val grayPaint: Paint

	// Error indicator is a red circle
	override fun draw(canvas: Canvas,gc:GraphicsConfiguration) {
		val radius = gc.scale*unscaledRadius
		val x = gc.originx + p2.x.toFloat()*gc.scale
		val y = gc.originy + p2.y.toFloat()*gc.scale
		canvas.drawCircle(x,y,radius,grayPaint)
	}

	init {
		grayPaint = Paint().apply { setARGB(255,200,200,200) }
		unscaledRadius = 10f
 	}
}
