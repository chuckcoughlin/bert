/**
 * Copyright 2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Canvas
import android.graphics.Paint
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bertspeak.data.Point2D
import chuckcoughlin.bertspeak.data.Side

/**
 * Draw a blue circle for a nose.
 * Draw a circular "head" centered on the nose.
 */
class NoseDrawable(joint: Joint, val p1:Point2D, p2: Point2D, side: Side) : LinkShapeDrawable(joint,p2,side) {
	val headRadius:Float
	val unscaledRadius:Float
	val blackPaint: Paint
	val bluePaint: Paint
	// Error indicator is a red circle
	override fun draw(canvas: Canvas,gc:GraphicsConfiguration) {
		var radius = gc.scale*headRadius
		val x = gc.originx + p2.x.toFloat()*gc.scale
		val y = gc.originy + p2.y.toFloat()*gc.scale
		canvas.drawCircle(x,y,radius,blackPaint)
		radius = gc.scale*unscaledRadius
		canvas.drawCircle(x,y,radius,bluePaint)
	}

	override val CLSS = "NoseDrawable"

	init {
		bluePaint = Paint().apply { setARGB(255,0,0,255) }
		blackPaint = Paint().apply { setARGB(255,240,240,240) }
		blackPaint.style = Paint.Style.STROKE
		blackPaint.strokeWidth = 5f
		unscaledRadius = 10f
		headRadius = 60f
 	}
}
