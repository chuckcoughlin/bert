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

 abstract class LinkShapeDrawable(nam:String,point2:Point2D,limbSide:Side): Drawable() {
	 val name:String
	 val p2:Point2D
	 val side:Side
	 var selected:Boolean
	var selectable:Boolean
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

	// This should always be overriden
	override fun draw(cas:Canvas) {
		Log.w(CLSS,"draw(): Illegal use.")
	}

	abstract fun draw(canvas: Canvas,gc:GraphicsConfiguration)
	fun isTouched(tp:Point2D):Boolean {
		var touched=false
		if( Math.abs(p2.x-tp.x) < TOLERANCE  &&
			Math.abs(p2.y-tp.y) < TOLERANCE ) {
			touched = true
		}
		return touched
	}

	val CLSS = "LinkShapeDrawable"
	val TOLERANCE  = 20f // Touch tolerance in screen units

	init {
		name = nam
		selected = false
		selectable = false
		p2 = point2
		side = limbSide
		opacity= PixelFormat.OPAQUE
		strokePaint.style = STROKE
	}
}
