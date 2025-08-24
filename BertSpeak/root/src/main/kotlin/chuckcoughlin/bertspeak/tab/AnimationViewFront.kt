/**
 * Copyright 2023-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import chuckcoughlin.bertspeak.data.Point2D
import chuckcoughlin.bertspeak.ui.graphics.GraphicsConfiguration
import chuckcoughlin.bertspeak.ui.graphics.LinkShapeDrawable
import chuckcoughlin.bertspeak.ui.graphics.Side

/**
 * View the robot skeleton looking straight on.
 */
class AnimationViewFront(context: Context, attrs: AttributeSet? = null)
                    : AnimationView(context,attrs) {
    private val name: String

    override fun draw(canvas:Canvas,gc:GraphicsConfiguration) {
        Log.i(name, String.format("onDraw ...."))
        canvas.drawPaint(configuration.backgroundColor)
        canvas.drawCircle(measuredWidth/2f,measuredHeight/2f,measuredWidth/5f,configuration.topColor)
        drawLinks(canvas,configuration)
    }

    // Select front first
    override fun selectDrawable(point: Point2D): LinkShapeDrawable? {
        var drawable: LinkShapeDrawable? = null
        for(d in drawables.values) {
            if(d.selectable && d.isTouched(point)) {
                drawable = d
                drawable.selected = true
                break
            }
        }
        return drawable
    }
    // Draw from back to front
     private fun drawLinks(canvas:Canvas,gc:GraphicsConfiguration) {
         for(drawable in drawables.values) {
             if( drawable.p1.x<0.0 ) {
                 drawable.draw(canvas,gc)
             }
         }
        for(drawable in drawables.values) {
            if( drawable.p1.x.equals(0.0)) {
                drawable.draw(canvas,gc)
            }
        }
        for(drawable in drawables.values) {
            if( drawable.p2.x>0.0 ) {
                drawable.draw(canvas,gc)
            }
        }
     }

    private val CLSS = "AnimationViewFront"

    init {
        name = CLSS
        configuration.projection = Side.FRONT
    }
}
