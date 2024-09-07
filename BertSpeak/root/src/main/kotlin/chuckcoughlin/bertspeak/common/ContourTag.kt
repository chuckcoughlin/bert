/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.common

import com.google.mlkit.vision.face.FaceContour

/**
 * Enumerate the potential contours available
 * from an ML FaceDetection. The delegate function
 * allows the code to be accessed, e.g.
 *     ContourType.LEFT_EAR.code
 */
enum class ContourTag(override val code:Int): CodeDelegate {
    FACE(FaceContour.FACE),                               // 1
    LEFT_CHEEK(FaceContour.LEFT_CHEEK),                   // 14
    LEFT_EYE(FaceContour.LEFT_EYE),                       // 6
    LEFT_EYEBROW_BOTTOM(FaceContour.LEFT_EYEBROW_BOTTOM), // 3
    LEFT_EYEBROW_TOP(FaceContour.LEFT_EYEBROW_TOP),       // 2
    LOWER_LIP_BOTTOM(FaceContour.LOWER_LIP_BOTTOM),       // 11
    LOWER_LIP_TOP(FaceContour.LOWER_LIP_TOP),             // 10
    NOSE_BOTTOM(FaceContour.NOSE_BOTTOM),                 // 13
    NOSE_BRIDGE(FaceContour.NOSE_BRIDGE),                 // 12
    RIGHT_CHEEK(FaceContour.RIGHT_CHEEK),                 // 15
    RIGHT_EYE(FaceContour.RIGHT_EYE),                     // 7
    RIGHT_EYEBROW_BOTTOM(FaceContour.RIGHT_EYEBROW_BOTTOM),// 5
    RIGHT_EYEBROW_TOP(FaceContour.RIGHT_EYEBROW_TOP),     // 4
    UPPER_LIP_BOTTOM(FaceContour.UPPER_LIP_BOTTOM),       // 9
    UPPER_LIP_TOP(FaceContour.UPPER_LIP_TOP),             // 8
    UNKNOWN(-1) ;                                   // Error


    companion object {
        /**
         * @return the contour corresponding the given code
         */
        fun tagForCode(code:Int) : ContourTag {
            var tag = UNKNOWN
            for (t in ContourTag.values()) {
                if( t.code == code ) {
                    tag = t
                    break
                }
            }
            return tag
        }
        /**
         * @return  a comma-separated list of the types in a single String.
         */
        fun names(): String {
            val names = StringBuffer()
            for (type in ContourTag.values()) {
                names.append(type.name + ", ")
            }
            return names.substring(0, names.length - 2)
        }
    }

}
