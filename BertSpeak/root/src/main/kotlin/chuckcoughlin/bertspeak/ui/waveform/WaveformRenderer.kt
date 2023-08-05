/**
 * @See: Mark Allison @ https://github.com/StylingAndroid/Visualizer
 */
package chuckcoughlin.bertspeak.ui.waveform

import android.graphics.Canvas

interface WaveformRenderer {
    fun render(canvas: Canvas, waveform: ByteArray?)
}
