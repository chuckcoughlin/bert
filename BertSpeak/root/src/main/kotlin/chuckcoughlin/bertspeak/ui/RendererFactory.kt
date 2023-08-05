/**
 * @See: Mark Allison @ https://github.com/StylingAndroid/Visualizer
 */
package chuckcoughlin.bertspeak.ui

import androidx.annotation.ColorInt
import chuckcoughlin.bertspeak.ui.waveform.SimpleWaveformRenderer
import chuckcoughlin.bertspeak.ui.waveform.WaveformRenderer

class RendererFactory {
    fun createSimpleWaveformRenderer(
        @ColorInt foreground: Int,
        @ColorInt background: Int
    ): WaveformRenderer {
        return SimpleWaveformRenderer.newInstance(background, foreground)
    }
}
