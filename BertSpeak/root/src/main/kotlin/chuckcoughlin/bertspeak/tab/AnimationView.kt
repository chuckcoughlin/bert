/**
 * Copyright 2023-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import chuckcoughlin.bertspeak.data.DefaultSkeleton
import chuckcoughlin.bertspeak.data.LinkLocation
import chuckcoughlin.bertspeak.data.Point2D
import chuckcoughlin.bertspeak.ui.graphics.GraphicsConfiguration
import chuckcoughlin.bertspeak.ui.graphics.LinkShapeDrawable
import chuckcoughlin.bertspeak.ui.graphics.ShapeFactory

/**
 * A canvas for displaying an entire skeleton in one of
 * FRONT, LEFT or RIGHT projections.
 */
 abstract class AnimationView(context: Context, attrs: AttributeSet? = null)
                    : View(context,attrs), View.OnTouchListener {
    var configuration: GraphicsConfiguration
    val drawables: MutableMap<String,LinkShapeDrawable>
    private var name: String
    private var selected: Boolean // Have we selected a joint or appendage
    private var skeleton: List<LinkLocation>

    fun clear() {
        drawables.clear()
    }

    override fun onDraw(canvas: Canvas) {
        draw(canvas,configuration)
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Log.i(CLSS, String.format("%s.onMeasure %dx%d (width,height)",configuration.projection.name,measuredWidth,measuredHeight))
        configuration.originx = measuredWidth/2f
        configuration.originy = measuredHeight/2f
        configuration.scale = measuredHeight / DefaultSkeleton.skeletoHeight
    }


    abstract fun draw(canvas: Canvas,gc:GraphicsConfiguration)

    fun updateDrawables(s:List<LinkLocation>) {
        skeleton = s
        for (loc in skeleton) {
            Log.i(CLSS, String.format("%s.updateDrawable %s", configuration.projection.name, loc.name))
            val drawable = ShapeFactory.drawableForLink(loc, configuration.projection)
            drawables[loc.name] = drawable
        }
    }

    /** ============= OnTouchListener ===============  */
    override fun onTouch(v:View,event: MotionEvent): Boolean {
        Log.i(CLSS, String.format("%s.onTouchEvent ==============================================",CLSS))
        val action = event.action
        // Retrieve the new x and y touch positions
        val x = event.x.toDouble()
        val y = event.y.toDouble()
        val spot = Point2D((x-configuration.originx)/configuration.scale,(y-configuration.originy)/configuration.scale)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                Log.i(CLSS, String.format("%s.onTouchEvent DOWN %dx%d",configuration.projection.name,x,y))
                findTouchedDrawable(spot)
            }
            MotionEvent.ACTION_MOVE -> {
                Log.i(CLSS, String.format("%s.onTouchEvent MOVE %dx%d",configuration.projection.name,x,y))
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                Log.i(CLSS, String.format("%s.onTouchEvent UP %dx%d",configuration.projection.name,x,y))
                performClick()
            }
        }
        return true
    }
    override fun performClick():Boolean {
        return true  // Consume the event
    }

    private fun findTouchedDrawable(point:Point2D):LinkShapeDrawable? {
        var drawable:LinkShapeDrawable? = null
        return drawable
    }

    private val CLSS = "AnimationView"
    val paintWidth = 8f

    init {
        name = CLSS
        configuration = GraphicsConfiguration()
        drawables = mutableMapOf<String,LinkShapeDrawable>()
        selected = false
        skeleton = mutableListOf<LinkLocation>()
    }
}
