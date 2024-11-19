/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * These are the canonical names for the skeletal sections ("bones")
 * of the humanoid. We try to be anatomically correct, but don't always succeed.
 *
 *  A bone translates to a "link" for purposes of location computations
 */
enum class Bone {
    CERVICAL, LEFT_CLAVICLE, LEFT_FOOT, LEFT_FOREARM, LEFT_HIP_LINK, LEFT_HIP_SOCKET, LEFT_ILLIUM, LEFT_SHIN, LEFT_SHOULDER_SOCKET, LEFT_THIGH, LEFT_UPPER_ARM,
    LOWER_SPINE, LUMBAR, NECK, PELVIS, RIGHT_CLAVICLE, RIGHT_FOOT, RIGHT_FOREARM, RIGHT_HIP_LINK, RIGHT_HIP_SOCKET, RIGHT_ILLIUM, RIGHT_SHIN, RIGHT_SHOULDER_SOCKET,
    RIGHT_THIGH, RIGHT_UPPER_ARM, SKULL, SPINE, THORACIC,
    NONE;

    companion object {
        /**
         * Convert the Limb enumeration to text that can be pronounced.
         * @param limb the enumeration
         * @return user-recognizable text
         */
        fun toText(limb: Bone): String {
            var text = ""
            when (limb) {
                CERVICAL -> text = "cervical vertibrae"
                LEFT_CLAVICLE -> text = "left collar bone"
                LEFT_FOOT -> text = "left foot"
                LEFT_FOREARM -> text = "left forearm"
                LEFT_HIP_LINK -> text = "left hip link"
                LEFT_HIP_SOCKET -> text = "left hip socket"
                LEFT_ILLIUM -> text = "left illium"
                LEFT_SHIN -> text = "left shin"      // shin
                LEFT_SHOULDER_SOCKET -> text = "left shoulder socket"
                LEFT_THIGH -> text = "left thigh"
                LEFT_UPPER_ARM -> text = "left upper arm"
                LOWER_SPINE -> text = "lower spine"
                LUMBAR -> text = "lumbar"
                NECK -> text = "neck"
                PELVIS -> text = "pelvis"
                RIGHT_CLAVICLE -> text = "right collar bone"
                RIGHT_FOOT -> text = "right foot"
                RIGHT_FOREARM -> text = "right forearm"
                RIGHT_HIP_LINK -> text = "right hip link"
                RIGHT_HIP_SOCKET -> text = "right hip socket"
                RIGHT_ILLIUM -> text = "right illium"
                RIGHT_SHIN -> text = "right shin"      // shin
                RIGHT_SHOULDER_SOCKET -> text = "right shoulder socket"
                RIGHT_THIGH -> text = "right thigh"
                RIGHT_UPPER_ARM -> text = "right upper arm"
                SKULL -> text = "skull"
                SPINE -> text = "spine"
                THORACIC -> text = "thoracic vertibrae"
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
        fun fromString(arg: String): Bone {
            for (bone in Bone.values()) {
                if (bone.name.equals(arg, true)) return bone
            }
            return Bone.NONE
        }
    }
}