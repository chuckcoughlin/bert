/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.ui.animate

import android.graphics.Canvas

interface AnimationRenderer {
    fun render(canvas: Canvas, waveform: ByteArray?)
}
