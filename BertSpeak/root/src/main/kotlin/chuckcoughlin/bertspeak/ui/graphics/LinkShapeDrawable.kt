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
import android.util.Log
import chuckcoughlin.bertspeak.data.LinkLocation
import chuckcoughlin.bertspeak.service.DispatchService.Companion.CLSS


/**
 * Abstract class to take care of all required overrides
 * except draw()
 */

 abstract class LinkShapeDrawable(lloc: LinkLocation): Drawable() {
	 val loc:LinkLocation
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
		loc = lloc
		opacity= PixelFormat.OPAQUE
		strokePaint.style = STROKE
	}
}
