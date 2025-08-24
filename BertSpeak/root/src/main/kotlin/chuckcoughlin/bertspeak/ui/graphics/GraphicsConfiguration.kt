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
	var backgroundColor: Paint
	var insideColor: Paint
	var outsideColor: Paint
	var topColor: Paint
	var bottomColor: Paint
	var errorColor: Paint
	var selectedColor: Paint
	var selectableColor: Paint
	var projection: Side
	var originx: Float   // Center of view
	var originy: Float
	var scale: Float   // Based on skeleton and view heights

	private val sWidth = 4f

	/**
	 * Default color scheme
	 */
	init {
		backgroundColor = Paint().apply {	setARGB(255,200,200,200)
											style = Paint.Style.FILL }
		insideColor      = Paint().apply {	setARGB(255,50,50,50)
											strokeWidth = sWidth
											style = Paint.Style.STROKE }
		outsideColor      = Paint().apply {	setARGB(255,255,255,255)
											strokeWidth = sWidth
											style = Paint.Style.STROKE }
		topColor          = Paint().apply {	setARGB(255,220,220,220)
											strokeWidth = sWidth
											style = Paint.Style.STROKE }
		bottomColor       = Paint().apply {	setARGB(255,0,0,0)
											strokeWidth = sWidth
											style = Paint.Style.STROKE }
		errorColor        = Paint().apply {	setARGB(255,255,0,0)
											style = Paint.Style.FILL }

		selectedColor     = Paint().apply {	setARGB(255,0,255,255)
											strokeWidth = sWidth
											style = Paint.Style.STROKE }
		selectableColor    = Paint().apply {	setARGB(255,255,255,0)
											style = Paint.Style.FILL }
		projection = Side.FRONT
		scale = 1.0f
		originx = 0.0f
		originy = 0.0f
	}
}
