/**
 * Copyright 2023-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.View
import chuckcoughlin.bertspeak.data.LinkLocation
import chuckcoughlin.bertspeak.ui.graphics.GraphicsConfiguration
import chuckcoughlin.bertspeak.ui.graphics.LinkShapeDrawable
import chuckcoughlin.bertspeak.ui.graphics.ShapeFactory

/**
 * A canvas for displaying an entire skeleton in one of
 * FRONT, LEFT or RIGHT projections.
 */
abstract class AnimationView(context: Context, attrs: AttributeSet? = null)
                    : View(context,attrs)  {
    var configuration: GraphicsConfiguration
    val drawables: MutableMap<String,LinkShapeDrawable>
    private var name: String

    fun clear() {
        drawables.clear()
    }

    override fun onDraw(canvas: Canvas) {
        draw(canvas,configuration)
    }

    abstract fun draw(canvas: Canvas,gc:GraphicsConfiguration)

    fun updateDrawable(loc: LinkLocation) {
        Log.i(configuration.projection.name, String.format("updateDrawable %s",loc.name))
        val drawable = ShapeFactory.drawableForLink(loc)
        drawables[loc.name] = drawable
    }

    private val CLSS = "AnimationView"

    init {
        name = CLSS
        configuration = GraphicsConfiguration()
        drawables = mutableMapOf<String,LinkShapeDrawable>()
    }
}
