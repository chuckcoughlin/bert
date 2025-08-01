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
		drawConnector(canvas,gc)
		drawBeginning(canvas,gc)
		drawEnd(canvas,gc)
	}

	private fun drawBeginning(canvas: Canvas,gc:GraphicsConfiguration) {
		val radius = gc.scale*beginningRadius
		val x = gc.originx + loc.source.x.toFloat()*gc.scale
		val y = gc.originy + loc.source.y.toFloat()*gc.scale
		canvas.drawCircle(x,y,radius,gc.foreground)
	}
	private fun drawEnd(canvas: Canvas,gc:GraphicsConfiguration) {
		val radius = gc.scale*endRadius
		val x = gc.originx + loc.end.x.toFloat()*gc.scale
		val y = gc.originy + loc.end.y.toFloat()*gc.scale
		canvas.drawCircle(x,y,radius,gc.foreground)
	}
	private fun drawConnector(canvas: Canvas,gc:GraphicsConfiguration) {
		var x1 = gc.originx + loc.source.x.toFloat()*gc.scale
		var y1 = gc.originy + loc.source.y.toFloat()*gc.scale
		var x2 = gc.originx + loc.end.x.toFloat()*gc.scale
		var y2 = gc.originy + loc.end.y.toFloat()*gc.scale
		if(x1>x2) {
			val tmp = x2
			x2 = x1
			x1 = tmp
		}
		if(y1>y2) {
			val tmp = y2
			y2 = y1
			y1 = tmp
		}
		val w = connectorWidth/2f
		//canvas.drawRect(x1-w ,y1,x2-w, y2,gc.foreground) // left,top,right,bottom
		canvas.drawArc(x1-w ,y1,x2-w, y2,0f,0f,true,gc.foreground) // left,top,right,bottom)
	}

	val beginningRadius= 10f
	val endRadius = 10f
	val connectorWidth= 4f
}
