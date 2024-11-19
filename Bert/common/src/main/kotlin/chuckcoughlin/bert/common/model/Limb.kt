/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * Limbs represent groups of joints/bones.
 */
enum class Limb {
    HEAD,  // These represent groups of joints ...
    LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG, TORSO, NONE;

    companion object {
        /**
         * Convert the Limb enumeration to text that can be pronounced.
         * @param limb the enumeration
         * @return user-recognizable text
         */
        fun toText(limb: Limb): String {
            var text = ""
            when (limb) {
                HEAD -> text = "head"
                LEFT_ARM -> text = "left arm"
                RIGHT_ARM -> text = "right arm"
                LEFT_LEG -> text = "left leg"
                RIGHT_LEG -> text = "right leg"
                TORSO -> text = "torso"
                NONE -> text = "none"
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

        /**
         * The enumeration function valueOf appears to always throw an exception.
         * This is the replacement. Case insensitive.
         */
        fun fromString(arg: String): Limb {
            for (limb in Limb.values()) {
                if (limb.name.equals(arg, true)) return limb
            }
            return Limb.NONE
        }
    }
}