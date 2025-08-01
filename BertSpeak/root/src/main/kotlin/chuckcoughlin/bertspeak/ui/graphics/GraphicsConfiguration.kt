/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Color
import android.graphics.Paint
import chuckcoughlin.bertspeak.ui.graphics.Side

/**
 * Holder parameters that affect the graphical rendering of the
 * robot's links.
 */
class GraphicsConfiguration () {
	var background: Paint
	var foreground: Paint
	var projection: Side
	var originx: Float   // Center of view
	var originy: Float
	var scale: Float   // Based on skeleton and view heights

	init {
		background = Paint()
		foreground = Paint()
		projection = Side.FRONT
		scale = 1.0f
		originx = 0.0f
		originy = 0.0f
	}
}
