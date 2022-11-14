/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * These are the canonical names for the skeletal sections of the humanoid.
 * We try to be anatomically correct, but don't always succeed.
 *
 * A second meaning is given to: left/right arms and legs and torso.
 * These represent a multi-jointed collection. (Pardon the double meaning,
 * I just ran out of names for things).
 */
enum class Limb {
    BACK, CERVICAL, HEAD, LEFT_CLAVICLE, LEFT_FOOT, LEFT_FOREARM, LEFT_HIP_LINK, LEFT_HIP_SOCKET, LEFT_ILLIUM, LEFT_SHIN, LEFT_SHOULDER_LINK, LEFT_THIGH, LEFT_UPPER_ARM, LOWER_SPINE, LUMBAR, PELVIS, RIGHT_CLAVICLE, RIGHT_FOOT, RIGHT_FOREARM, RIGHT_HIP_LINK, RIGHT_HIP_SOCKET, RIGHT_ILLIUM, RIGHT_SHIN, RIGHT_SHOULDER_LINK, RIGHT_THIGH, RIGHT_UPPER_ARM, SPINE, THORACIC,  // These represent groups of joints ...
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
                BACK -> text = "back"
                CERVICAL -> text = "cervical vertibrae"
                HEAD -> text = "head"
                LEFT_CLAVICLE -> text = "left collar bone"
                LEFT_FOOT -> text = "left foot"
                LEFT_FOREARM -> text = "left forearm"
                LEFT_HIP_LINK -> text = "left hip link"
                LEFT_HIP_SOCKET -> text = "left hip socket"
                LEFT_ILLIUM -> text = "left illium"
                LEFT_SHIN -> text = "left shin"
                LEFT_SHOULDER_LINK -> text = "left shoulder link"
                LEFT_THIGH -> text = "left thigh"
                LEFT_UPPER_ARM -> text = "left upper arm"
                LOWER_SPINE -> text = "lower spine"
                LUMBAR -> text = "lumbar"
                PELVIS -> text = "pelvis"
                RIGHT_CLAVICLE -> text = "right collar bone"
                RIGHT_FOOT -> text = "right foot"
                RIGHT_FOREARM -> text = "right forearm"
                RIGHT_HIP_LINK -> text = "right hip link"
                RIGHT_HIP_SOCKET -> text = "right hip socket"
                RIGHT_ILLIUM -> text = "right illium"
                RIGHT_SHIN -> text = "right shin"
                RIGHT_SHOULDER_LINK -> text = "right shoulder link"
                RIGHT_THIGH -> text = "right thigh"
                RIGHT_UPPER_ARM -> text = "right upper arm"
                SPINE -> text = "spine"
                THORACIC -> text = "thoracic vertibrae"
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
    }
}