/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.util.TextUtility

/**
 * These are the canonical names for appendages or end effectors
 * on various limbs. These are body parts where we need locations.
 * At this point there are no actions associated with them.
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
        /** @return  a comma-separated list of common names for the extremities.
         */
        fun nameList(): String {
            val list = mutableListOf<String>()
            for (ext in Appendage.values()) {
                if( ext==Appendage.NONE) continue
                list.add(Appendage.toText(ext))
            }
            return TextUtility.createTextForSpeakingFromList(list)
        }
        /**
         * The enumeration function valueOf appears to always throw an exception.
         * This is the replacement. It is case-insensitive,
         */
        fun fromString(arg: String): Appendage {
            for (type in Appendage.values()) {
                if (type.name.equals(arg, true)) return type
            }
            return Appendage.NONE
        }
    }
}