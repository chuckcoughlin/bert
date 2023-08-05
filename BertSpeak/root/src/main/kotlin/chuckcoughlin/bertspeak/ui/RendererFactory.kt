/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 * @See: Mark Allison @ https://github.com/StylingAndroid/Visualizer
 */
package chuckcoughlin.bertspeak.ui

import androidx.annotation.ColorInt
import chuckcoughlin.bertspeak.ui.waveform.SimpleWaveformRenderer
import chuckcoughlin.bertspeak.ui.waveform.WaveformRenderer
import chuckcoughlin.bertspeak.waveform.AnimationRenderer
import chuckcoughlin.bertspeak.waveform.FacialRecognitionRenderer
import chuckcoughlin.bertspeak.waveform.SimpleAnimationRenderer
import chuckcoughlin.bertspeak.waveform.SimpleFacialRecognitionRenderer

class RendererFactory {
    fun createSimpleAnimationRenderer(@ColorInt foreground: Int,@ColorInt background: Int): AnimationRenderer {
        return SimpleAnimationRenderer.newInstance(background, foreground)
    }
    fun createSimpleFacialRecognitionRenderer(@ColorInt foreground: Int,@ColorInt background: Int): FacialRecognitionRenderer {
        return SimpleFacialRecognitionRenderer.newInstance(background, foreground)
    }
    fun createSimpleWaveformRenderer(@ColorInt foreground: Int,@ColorInt background: Int): WaveformRenderer {
        return SimpleWaveformRenderer.newInstance(background, foreground)
    }
}
