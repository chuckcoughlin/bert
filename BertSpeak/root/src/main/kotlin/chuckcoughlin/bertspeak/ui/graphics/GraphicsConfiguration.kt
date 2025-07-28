/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Color
import android.graphics.Paint
import chuckcoughlin.bertspeak.ui.graphics.Side

/**
 * This class is a holder for parameters that
 * affect the graphical rendering of the
 * robot's links.
 */
class GraphicsConfiguration () {
	var background: Paint
	var foreground: Paint
	var projection: Side
	var scale: Double

	init {
		background = Paint()
		foreground = Paint()
		projection = Side.FRONT
		scale = 0.1
	}
}
