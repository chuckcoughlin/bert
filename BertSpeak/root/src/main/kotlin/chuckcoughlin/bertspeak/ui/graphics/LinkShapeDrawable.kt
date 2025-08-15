/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Paint.Style.STROKE
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.Log
import chuckcoughlin.bertspeak.data.Point2D


/**
 * Abstract class to take care of all required overrides
 * except draw()
 */

 abstract class LinkShapeDrawable(point1: Point2D,point2:Point2D,limbSide:Side): Drawable() {
	 val p1:Point2D
	 val p2:Point2D
	 val side:Side
	 private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
	 private val opacity: Int


	@Deprecated("Proper drawable not found")
	override fun getOpacity(): Int {
		return opacity
	}
	override fun setAlpha(p0: Int) {
	}

	override fun setColorFilter(p0: ColorFilter?) {
		TODO("Not yet implemented")
	}

	// This should never be called?
	override fun draw(cas:Canvas) {
		Log.w(CLSS,"draw(): Illegal use.")
	}

	abstract  fun draw(canvas: Canvas,gc:GraphicsConfiguration)

	val CLSS = "LinkShapeDrawable"

	init {
		p1 = point1
		p2 = point2
		side = limbSide
		opacity= PixelFormat.OPAQUE
		strokePaint.style = STROKE
	}
}
