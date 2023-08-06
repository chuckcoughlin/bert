/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.animate

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.annotation.ColorInt
import chuckcoughlin.bertspeak.ui.animate.AnimationRenderer

internal class SimpleAnimationRenderer(
    @field:ColorInt @param:ColorInt private val backgroundColour: Int,private val foregroundPaint: Paint,
    private val waveformPath: Path) : AnimationRenderer {
    override fun render(canvas: Canvas, waveform: ByteArray?) {
        canvas.drawColor(backgroundColour)
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        waveformPath.reset()
        if (waveform != null) {
            renderAnimation(waveform, width, height)
        }
        else {
            renderBlank(width, height)
        }
        canvas.drawPath(waveformPath, foregroundPaint)
    }

    private fun renderAnimation(waveform: ByteArray, width: Float, height: Float) {
        val xIncrement = width / waveform.size.toFloat()
        val yIncrement = height / Y_FACTOR
        val halfHeight = (height * HALF_FACTOR).toInt()
        waveformPath.moveTo(0f, halfHeight.toFloat())
        for (i in 1 until waveform.size) {
            val yPosition =
                if (waveform[i] > 0) height - yIncrement * waveform[i] else -(yIncrement * waveform[i])
            waveformPath.lineTo(xIncrement * i, yPosition)
        }
        waveformPath.lineTo(width, halfHeight.toFloat())
    }

    private fun renderBlank(width: Float, height: Float) {
        val y = (height * HALF_FACTOR).toInt()
        waveformPath.moveTo(0f, y.toFloat())
        waveformPath.lineTo(width, y.toFloat())
    }

    companion object {
        private const val Y_FACTOR = 0xFF
        private const val HALF_FACTOR = 0.5f
        fun newInstance(
            @ColorInt backgroundColour: Int,
            @ColorInt foregroundColour: Int
        ): SimpleAnimationRenderer {
            val paint = Paint()
            paint.color = foregroundColour
            paint.isAntiAlias = true
            paint.style = Paint.Style.STROKE
            val waveformPath = Path()
            return SimpleAnimationRenderer(backgroundColour, paint, waveformPath)
        }
    }
}
