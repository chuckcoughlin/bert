/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.waveform

import android.graphics.Canvas

interface FacialRecognitionRenderer {
    fun render(canvas: Canvas, waveform: ByteArray?)
}
