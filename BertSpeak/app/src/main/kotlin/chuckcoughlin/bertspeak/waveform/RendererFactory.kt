/**
 * @See: Mark Allison @ https://github.com/StylingAndroid/Visualizer
 */
package chuckcoughlin.bertspeak.waveform

import androidx.annotation.ColorInt

class RendererFactory {
    fun createSimpleWaveformRenderer(
        @ColorInt foreground: Int,
        @ColorInt background: Int
    ): WaveformRenderer {
        return SimpleWaveformRenderer.Companion.newInstance(background, foreground)
    }
}
