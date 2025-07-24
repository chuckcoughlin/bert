/**
 * Copyright 2023-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.View
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.data.LinkLocation
import chuckcoughlin.bertspeak.data.LinkShapeObserver
import chuckcoughlin.bertspeak.data.LogData
import chuckcoughlin.bertspeak.service.ManagerState
import chuckcoughlin.bertspeak.ui.graphics.GraphicsConfiguration
import chuckcoughlin.bertspeak.ui.graphics.LinkShapeDrawable
import chuckcoughlin.bertspeak.ui.graphics.ShapeFactory
import chuckcoughlin.bertspeak.ui.graphics.Side
import chuckcoughlin.bertspeak.ui.graphics.Side.BACK
import chuckcoughlin.bertspeak.ui.graphics.Side.FRONT
import chuckcoughlin.bertspeak.ui.graphics.Side.LEFT
import chuckcoughlin.bertspeak.ui.graphics.Side.RIGHT

/**
 * A canvas for displaying an entire skeleton in one of
 * FRONT, LEFT or RIGHT projections.
 */
class AnimationView(context: Context, attrs: AttributeSet? = null)
                    : View(context,attrs), LinkShapeObserver {
    override val configuration: GraphicsConfiguration
    private var drawablesFront: MutableList<LinkShapeDrawable>
    private var drawablesLeft:  MutableList<LinkShapeDrawable>
    private var drawablesRight: MutableList<LinkShapeDrawable>
    override val name: String
    var activity: Activity

    /**
     * Draw in three passes to get proper overly
     * for the particular projection
     */
    override fun onDraw(cas:Canvas) {
        when(configuration.projection) {
            RIGHT-> {
                for(drawable in drawablesLeft) {
                    drawable.draw(cas)
                }
                for(drawable in drawablesFront) {
                    drawable.draw(cas)
                }
                for(drawable in drawablesRight) {
                    drawable.draw(cas)
                }
            }
            LEFT -> {
                for(drawable in drawablesRight) {
                    drawable.draw(cas)
                }
                for(drawable in drawablesFront) {
                    drawable.draw(cas)
                }
                for(drawable in drawablesLeft) {
                    drawable.draw(cas)
                }
            }
            FRONT-> {
                for(drawable in drawablesLeft) {
                    drawable.draw(cas)
                }
                for(drawable in drawablesRight) {
                    drawable.draw(cas)
                }
                for(drawable in drawablesFront) {
                    drawable.draw(cas)
                }
            }
            BACK -> {}
            }
    }


    // Nothing
    override fun resetGraphics() {
    }

    override fun updateGraphics(skeleton:List<LinkLocation>) {
        drawablesRight.clear()
        drawablesLeft.clear()
        drawablesFront.clear()
        Log.i(name, String.format("updateGraphics %d elements in skeleton",skeleton.size))
        for(loc in skeleton) {
            val drawable  = ShapeFactory.drawableForLink(loc,configuration)
            val side = Side.fromString(loc.side)
            when(side) {
               FRONT-> drawablesFront.add(drawable)
               LEFT->drawablesLeft.add(drawable)
               RIGHT->drawablesRight.add(drawable)
               BACK->{}
            }
        }
        reDraw(drawablesFront)
        reDraw(drawablesLeft)
        reDraw(drawablesRight)
    }

    /**
     * Force a redraw
     */
    fun reDraw(list: List<LinkShapeDrawable>) {
        activity.runOnUiThread {
            for( drawable in list)
            invalidateDrawable(drawable)
        }
    }
    val CLSS = "AnimationView"

    init {
        name = CLSS
        activity = Activity()  // Temporary
        configuration = GraphicsConfiguration()
        drawablesFront = mutableListOf<LinkShapeDrawable>()
        drawablesLeft = mutableListOf<LinkShapeDrawable>()
        drawablesRight = mutableListOf<LinkShapeDrawable>()
    }
}
