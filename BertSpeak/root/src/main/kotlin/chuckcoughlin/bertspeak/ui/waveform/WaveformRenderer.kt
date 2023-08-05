/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.ui.waveform

import android.graphics.Canvas

interface WaveformRenderer {
    fun render(canvas: Canvas, waveform: ByteArray?)
}
