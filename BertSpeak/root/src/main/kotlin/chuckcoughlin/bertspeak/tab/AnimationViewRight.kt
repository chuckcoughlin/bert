/**
 * Copyright 2023-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import chuckcoughlin.bertspeak.data.LinkLocation
import chuckcoughlin.bertspeak.ui.graphics.GraphicsConfiguration
import chuckcoughlin.bertspeak.ui.graphics.LinkShapeDrawable
import chuckcoughlin.bertspeak.ui.graphics.ShapeFactory
import chuckcoughlin.bertspeak.ui.graphics.Side

/**
 * View robot skeleton from the right.
 */
class AnimationViewRight(context: Context, attrs: AttributeSet? = null)
                    : AnimationView(context,attrs)  {
    private val name: String

    override fun draw(canvas:Canvas,gc:GraphicsConfiguration) {
        Log.i(name, String.format("onDraw ...."))
        canvas.drawPaint(configuration.background)
        canvas.drawCircle(measuredWidth/2f,measuredHeight/2f,measuredWidth/5f,configuration.foreground)
        drawLinks(canvas,configuration)
    }

    // Draw from left to right
    private fun drawLinks(canvas:Canvas,gc:GraphicsConfiguration) {
        for(drawable in drawables.values) {
            if( drawable.loc.side.equals(Side.LEFT.name,true)) {
                drawable.draw(canvas,gc)
            }
        }
        for(drawable in drawables.values) {
            if( drawable.loc.side.equals(Side.FRONT.name,true)) {
                drawable.draw(canvas,gc)
            }
        }
        for(drawable in drawables.values) {
            if( drawable.loc.side.equals(Side.RIGHT.name,true)) {
                drawable.draw(canvas,gc)
            }
        }
    }

    private val CLSS = "AnimationViewRight"
    private val sWidth =  6f

    init {
        name = CLSS
        configuration.projection = Side.RIGHT
        configuration.background = Paint().apply { setARGB(255,100,100,100)
                                                   style = Paint.Style.FILL}
        configuration.foreground = Paint().apply { setARGB(255,255,255,0)
                                                   strokeWidth = sWidth
                                                   style = Paint.Style.STROKE}
    }
}
