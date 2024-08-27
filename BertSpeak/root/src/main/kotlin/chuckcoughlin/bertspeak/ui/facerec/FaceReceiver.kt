/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.ui.facerec

import android.graphics.Canvas
import com.google.mlkit.vision.face.Face

interface FaceReceiver {
    fun acceptFace(face:Face)
}
