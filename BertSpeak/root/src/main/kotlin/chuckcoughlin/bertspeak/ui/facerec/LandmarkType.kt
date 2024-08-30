/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.ui.facerec

import com.google.mlkit.vision.face.FaceLandmark

/**
 * Enumerate the potential landmarks available
 * from an ML FaceDetection. The delegate function
 * allows the code to be accessed, e.g.
 *     LandmarkType.LEFT_EAR.code
 */
enum class LandmarkType( override val code:Int):CodeDelegate {
    LEFT_CHEEK(FaceLandmark.LEFT_CHEEK),     // 1
    LEFT_EAR(FaceLandmark.LEFT_EAR),         // 3
    LEFT_EYE(FaceLandmark.LEFT_EYE),         // 4
    MOUTH_BOTTOM(FaceLandmark.MOUTH_BOTTOM), // 0
    MOUTH_LEFT(FaceLandmark.MOUTH_LEFT),     // 5
    MOUTH_RIGHT(FaceLandmark.MOUTH_RIGHT),   // 11
    NOSE_BASE(FaceLandmark.NOSE_BASE),       // 6
    RIGHT_CHEEK(FaceLandmark.RIGHT_CHEEK),   // 7
    RIGHT_EAR(FaceLandmark.RIGHT_EAR),       // 9
    RIGHT_EYE(FaceLandmark.RIGHT_EYE);      // 10


    companion object {
        /**
         * @return  a comma-separated list of the types in a single String.
         */
        fun names(): String {
            val names = StringBuffer()
            for (type in LandmarkType.values()) {
                names.append(type.name + ", ")
            }
            return names.substring(0, names.length - 2)
        }
    }

}
