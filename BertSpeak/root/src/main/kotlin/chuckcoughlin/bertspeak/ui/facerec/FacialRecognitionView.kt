/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.ui.facerec

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import chuckcoughlin.bertspeak.ui.facerec.FacialRecognitionRenderer
import java.util.Arrays

class FacialRecognitionView : View {
    private var waveform: ByteArray = ByteArray(120)
    private var renderer: FacialRecognitionRenderer? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setRenderer(renderer: FacialRecognitionRenderer?) {
        this.renderer = renderer
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (renderer != null) {
            renderer!!.render(canvas, waveform)
        }
    }

    fun setWaveform(waveform: ByteArray) {
        this.waveform = Arrays.copyOf(waveform, waveform.size)
        invalidate()
    }
}
