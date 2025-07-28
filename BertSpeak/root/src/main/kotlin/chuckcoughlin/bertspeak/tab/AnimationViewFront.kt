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
 * View the robot skeleton looking straight on.
 */
class AnimationViewFront(context: Context, attrs: AttributeSet? = null)
                    : AnimationView(context,attrs)  {
    private val name: String

    override fun draw(canvas:Canvas,gc:GraphicsConfiguration) {
        Log.i(name, String.format("onDraw ...."))
        canvas.drawPaint(configuration.background)
        drawLinks(canvas,configuration)
    }

    // Draw from back to front
     private fun drawLinks(canvas:Canvas,gc:GraphicsConfiguration) {
         for(drawable in drawables.values) {
             if( drawable.loc.source.x<0.0 ) {
                 drawable.draw(canvas,gc)
             }
         }
        for(drawable in drawables.values) {
            if( drawable.loc.source.x.equals(0.0)) {
                drawable.draw(canvas,gc)
            }
        }
        for(drawable in drawables.values) {
            if( drawable.loc.source.x>0.0 ) {
                drawable.draw(canvas,gc)
            }
        }
     }

    private val CLSS = "AnimationViewFront"

    init {
        name = CLSS
        configuration.projection = Side.FRONT
        configuration.background = Paint().apply { setARGB(255,0,0,0) }
        configuration.foreground = Paint().apply { setARGB(255,200,200,200) }
    }
}
