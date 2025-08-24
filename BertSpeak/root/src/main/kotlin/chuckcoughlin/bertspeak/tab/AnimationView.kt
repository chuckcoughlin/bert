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
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.ui.graphics.GraphicsConfiguration
import chuckcoughlin.bertspeak.ui.graphics.LinkShapeDrawable
import chuckcoughlin.bertspeak.ui.graphics.ShapeFactory
import chuckcoughlin.bertspeak.ui.graphics.Side.BACK
import chuckcoughlin.bertspeak.ui.graphics.Side.FRONT
import chuckcoughlin.bertspeak.ui.graphics.Side.LEFT
import chuckcoughlin.bertspeak.ui.graphics.Side.RIGHT

/**
 * A canvas for displaying an entire skeleton in one of
 * FRONT, LEFT or RIGHT projections.
 */
 abstract class AnimationView(context: Context, attrs: AttributeSet? = null)
                    : View(context,attrs), View.OnTouchListener {
    var canvas:Canvas
    var configuration: GraphicsConfiguration
    val drawables: MutableMap<String,LinkShapeDrawable>
    private var name: String
    private var selected: LinkShapeDrawable? // Have we selected a joint or appendage

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
    /*
     * Test the skeleton, returning the first drawable within limits of
     * the touch point, if any. Test only the joint or appendage.
     */
     abstract fun selectDrawable(point:Point2D):LinkShapeDrawable?

    fun updateDrawables(skeleton:List<LinkLocation>) {
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
        // Retrieve the new x and y touch positions in view screen coordinates
        val x = event.x.toDouble()
        val y = event.y.toDouble()
        val screenCoordinates = Point2D(x,y)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                selected = selectDrawable(screenCoordinates)
                if( selected!=null) {
                    Log.i(CLSS, String.format("%s.onTouchEvent DOWN %dx%d selected %s",configuration.projection.name,x,y,selected!!.name))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if(selected!=null) {
                    Log.i(CLSS, String.format("%s.onTouchEvent MOVE %dx%d", configuration.projection.name, x, y))
                    selected!!.draw(canvas,configuration)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                Log.i(CLSS, String.format("%s.onTouchEvent UP %dx%d",configuration.projection.name,x,y))
                performClick()
                if(selected!=null) {
                    val physical = Point2D((x-configuration.originx)/configuration.scale,(y-configuration.originy)/configuration.scale)
                    val location = DispatchService.linkLocationByName(selected!!.name)
                    location.end.z = physical.y
                    when(configuration.projection) {
                        BACK  -> location.end.y = -physical.x
                        FRONT -> location.end.y = physical.x
                        LEFT  -> location.end.x = -physical.x
                        RIGHT -> location.end.x = physical.x
                    }
                    DispatchService.updateJointPosition(location)
                }
                selected = null
            }
        }
        return true
    }
    override fun performClick():Boolean {
        return true  // Consume the event
    }



    private val CLSS = "AnimationView"
    val PAINT_WIDTH = 8f

    init {
        name = CLSS
        canvas = Canvas()
        configuration = GraphicsConfiguration()
        drawables = mutableMapOf<String,LinkShapeDrawable>()
        selected = null
    }
}
