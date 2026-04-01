/**
 * Copyright 2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import chuckcoughlin.bertspeak.data.Point2D
import chuckcoughlin.bertspeak.ui.graphics.GraphicsConfiguration
import chuckcoughlin.bertspeak.ui.graphics.LinkShapeDrawable
import chuckcoughlin.bertspeak.ui.graphics.Side

/**
 * View robot skeleton from the right.
 */
class AnimationViewRight(context: Context, attrs: AttributeSet? = null)
                    : AnimationView(context,attrs)  {
    private val name: String

    override fun draw(canvas:Canvas,gc:GraphicsConfiguration) {
        Log.i(name, String.format("onDraw RIGHT ..."))
        canvas.drawPaint(configuration.backgroundColor)
        canvas.drawCircle(measuredWidth/2f,measuredHeight/2f,measuredWidth/5f,configuration.topColor)
        drawLinks(canvas,configuration)
    }

    // Only select from right side or center
    override fun selectDrawable(point: Point2D): LinkShapeDrawable? {
        var drawable: LinkShapeDrawable? = null
        for(d in drawables.values) {
            if(d.selectable && d.side==Side.RIGHT && d.isTouched(point)) {
                drawable = d
                drawable.selected = true
                break
            }
        }
        if( drawable==null) {
            for(d in drawables.values) {
                if(d.selectable && d.side == Side.FRONT && d.isTouched(point)) {
                    drawable = d
                    drawable.selected = true
                    break
                }
            }
        }
        return drawable
    }

    // Draw limbs so left is on bottom, right on top
    private fun drawLinks(canvas:Canvas,gc:GraphicsConfiguration) {
        var count = 0
        var MAX_LIMBS = 6
        for(drawable in drawables.values) {
            if(count>MAX_LIMBS) break
            if( drawable.side==Side.LEFT) {
                drawable.draw(canvas,gc)
                count++
            }
        }
        for(drawable in drawables.values) {
            if(count>MAX_LIMBS) break
            if( drawable.side==Side.FRONT) {
                drawable.draw(canvas,gc)
                count++
            }
        }
        for(drawable in drawables.values) {
            if(count>MAX_LIMBS) break
            if( drawable.side==Side.RIGHT) {
                drawable.draw(canvas,gc)
                count++
            }
        }
    }

    private val CLSS = "AnimationViewRight"
    private val sWidth =  6f

    init {
        name = CLSS
        configuration.projection = Side.RIGHT
    }
}
