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
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import chuckcoughlin.bertspeak.data.LinkLocation


/**
 * Abstract class to take care of all required overrides
 * except draw()
 */

 abstract class LinkShapeDrawable(): Drawable() {
	private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val opacity: Int


	@Deprecated("Proper drawable not found")
	override fun getOpacity(): Int {
		return opacity
	}
	fun getStrokePaint(): Paint {
		return strokePaint
	}
	override fun setAlpha(p0: Int) {
	}

	override fun setColorFilter(p0: ColorFilter?) {
		TODO("Not yet implemented")
	}


	init {
		opacity= PixelFormat.OPAQUE
		strokePaint.style = STROKE
	}
}