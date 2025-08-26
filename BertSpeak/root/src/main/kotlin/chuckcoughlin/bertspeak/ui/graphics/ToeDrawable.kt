/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import chuckcoughlin.bertspeak.data.Point2D

/**
 * The ankle connector is very short. Draw it and a toe image
 */
class ToeDrawable(name:String, p1:Point2D, p2:Point2D, side:Side) : LinkShapeDrawable(name,p1,p2,side) {

	//
	override fun draw(canvas: Canvas,gc:GraphicsConfiguration) {
		drawConnector(canvas,gc)
		drawEnd(canvas,gc)
	}

	private fun drawEnd(canvas: Canvas,gc:GraphicsConfiguration) {
		val radius = gc.scale*endRadius
		val x = gc.originx + p2.x.toFloat()*gc.scale
		val y = gc.originy + p2.y.toFloat()*gc.scale
		canvas.drawCircle(x,y,radius,if(selected) gc.selectedColor else gc.topColor)
	}
	private fun drawConnector(canvas: Canvas,gc:GraphicsConfiguration) {
		var x1 = gc.originx + p1.x.toFloat()*gc.scale
		var y1 = gc.originy + p1.y.toFloat()*gc.scale
		var x2 = gc.originx + p2.x.toFloat()*gc.scale
		var y2 = gc.originy + p2.y.toFloat()*gc.scale

		val paint = if(selected) Paint(gc.selectedColor) else Paint(gc.selectableColor)
		paint.strokeWidth = connectorWidth
		canvas.drawLine(x1,y1,x2,y2,paint)
	}

	val endRadius = 10f
	val connectorWidth= 10f

	init {
		selectable = true
	}
}
