/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import chuckcoughlin.bertspeak.ui.graphics.Side.FRONT

/**
 * This class is a holder for parameters that
 * affect the graphical rendering of the
 * robot's links.
 */
class GraphicsConfiguration () {
	var projection: Side
	var scale: Double

	init {
		projection = FRONT
		scale = 0.1
	}
}