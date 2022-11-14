/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * These are the canonical names for appendages on various limbs.
 * These are body parts where we need locations.
 */
enum class Appendage {
    LEFT_EAR, LEFT_EYE, LEFT_FINGER, LEFT_HEEL, LEFT_TOE, NOSE,
    RIGHT_EAR, RIGHT_EYE, RIGHT_FINGER, RIGHT_HEEL, RIGHT_TOE, NONE;

    companion object {
        /**
         * Convert the Limb enumeration to text that can be pronounced.
         * @param limb the enumeration
         * @return user-recognizable text
         */
        fun toText(limb: Appendage): String {
            var text = ""
            when (limb) {
                LEFT_EAR -> text = "left ear"
                LEFT_EYE -> text = "left eye"
                LEFT_FINGER -> text = "left finger"
                LEFT_HEEL -> text = "left heel"
                LEFT_TOE -> text = "left toe"
                NOSE -> text = "nose"
                RIGHT_EAR -> text = "right ear"
                RIGHT_EYE -> text = "right eye"
                RIGHT_FINGER -> text = "right finger"
                RIGHT_HEEL -> text = "right heel"
                RIGHT_TOE -> text = "right toe"
                NONE -> text = "unknown"
            }
            return text
        }

        /**
         * @return  a comma-separated list of all block states in a single String.
         */
        fun names(): String {
            val names = StringBuffer()
            for (type in values()) {
                names.append(type.name + ", ")
            }
            return names.substring(0, names.length - 2)
        }
    }
}