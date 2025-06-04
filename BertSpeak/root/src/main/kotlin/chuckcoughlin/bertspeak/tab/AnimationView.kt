/**
 * Copyright 2023-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.Shape
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.view.View
import chuckcoughlin.bertspeak.data.LinkLocation
import chuckcoughlin.bertspeak.data.LinkShapeObserver
import chuckcoughlin.bertspeak.service.DispatchService.Companion.CLSS
import chuckcoughlin.bertspeak.ui.graphics.GraphicsConfiguration
import chuckcoughlin.bertspeak.ui.graphics.LinkShapeDrawable
import chuckcoughlin.bertspeak.ui.graphics.ShapeFactory
import chuckcoughlin.bertspeak.ui.graphics.Side
import chuckcoughlin.bertspeak.ui.graphics.Side.BACK
import chuckcoughlin.bertspeak.ui.graphics.Side.Companion
import chuckcoughlin.bertspeak.ui.graphics.Side.FRONT
import chuckcoughlin.bertspeak.ui.graphics.Side.LEFT
import chuckcoughlin.bertspeak.ui.graphics.Side.RIGHT

/**
 * A canvas for displaying an entire skeleton in one of
 * FRONT, LEFT or RIGHT projections.
 */
class AnimationView(context: Context): View(context), LinkShapeObserver {
    override val configuration: GraphicsConfiguration
    private var drawablesFront: MutableList<LinkShapeDrawable>
    private var drawablesLeft: MutableList<LinkShapeDrawable>
    private var drawablesRight: MutableList<LinkShapeDrawable>
    override val name: String

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
        drawablesRight = mutableListOf<LinkShapeDrawable>()
        drawablesLeft = mutableListOf<LinkShapeDrawable>()
        drawablesFront = mutableListOf<LinkShapeDrawable>()
        for(loc in skeleton) {
            val drawable  = ShapeFactory.drawableForLink(loc,configuration)
            val side = Side.fromString(loc.side)
            when(side) {
               FRONT-> drawablesFront.add(drawable)
               LEFT->drawablesFront.add(drawable)
               RIGHT->drawablesFront.add(drawable)
               BACK->{}
            }
        }
    }

    val CLSS = "AnimationView"

    init {
        name = CLSS
        configuration = GraphicsConfiguration()
        drawablesFront = mutableListOf<LinkShapeDrawable>()
        drawablesLeft = mutableListOf<LinkShapeDrawable>()
        drawablesRight = mutableListOf<LinkShapeDrawable>()
    }
}
