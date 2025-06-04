/**
 * Copyright 2023-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 * @See: Mark Allison @ https://github.com/StylingAndroid/Visualizer
 */
package chuckcoughlin.bertspeak.ui

import androidx.annotation.ColorInt
import chuckcoughlin.bertspeak.ui.waveform.SimpleWaveformRenderer
import chuckcoughlin.bertspeak.ui.waveform.WaveformRenderer
import chuckcoughlin.bertspeak.ui.facerec.FacialRecognitionRenderer
import chuckcoughlin.bertspeak.ui.facerec.SimpleFacialRecognitionRenderer

class RendererFactory {
    fun createSimpleFacialRecognitionRenderer(@ColorInt foreground: Int,@ColorInt background: Int): FacialRecognitionRenderer {
        return SimpleFacialRecognitionRenderer.newInstance(background, foreground)
    }
    fun createSimpleWaveformRenderer(@ColorInt foreground: Int,@ColorInt background: Int): WaveformRenderer {
        return SimpleWaveformRenderer.newInstance(background, foreground)
    }
}
