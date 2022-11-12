/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * These are the canonical names for the joints of the humanoid.
 */
enum class Joint {
    ABS_X, ABS_Y, ABS_Z, BUST_X, BUST_Y, NECK_Y, NECK_Z, LEFT_ANKLE_Y, LEFT_ARM_Z, LEFT_ELBOW_Y, LEFT_HIP_X, LEFT_HIP_Y, LEFT_HIP_Z, LEFT_KNEE_Y, LEFT_SHOULDER_X, LEFT_SHOULDER_Y, RIGHT_ANKLE_Y, RIGHT_ARM_Z, RIGHT_ELBOW_Y, RIGHT_HIP_X, RIGHT_HIP_Y, RIGHT_HIP_Z, RIGHT_KNEE_Y, RIGHT_SHOULDER_X, RIGHT_SHOULDER_Y, UNKNOWN;

    companion object {
        /**
         * Convert the Joint enumeration to text that can be pronounced.
         * @param joint the enumeration
         * @return user-recognizable text
         */
        fun toText(joint: Joint): String {
            var text = ""
            when (joint) {
                ABS_X -> text = "abdomen x"
                ABS_Y -> text = "abdomen y"
                ABS_Z -> text = "abdomen z"
                BUST_X -> text = "chest horizontal"
                BUST_Y -> text = "chest vertical"
                NECK_Y -> text = "neck y"
                NECK_Z -> text = "neck z"
                LEFT_ANKLE_Y -> text = "left ankle"
                LEFT_ARM_Z -> text = "left arm z"
                LEFT_ELBOW_Y -> text = "left elbow"
                LEFT_HIP_X -> text = "left hip x"
                LEFT_HIP_Y -> text = "left hip y"
                LEFT_HIP_Z -> text = "left hip z"
                LEFT_KNEE_Y -> text = "left knee"
                LEFT_SHOULDER_X -> text = "left shoulder horizontal"
                LEFT_SHOULDER_Y -> text = "left shoulder vertical"
                RIGHT_ANKLE_Y -> text = "right ankle"
                RIGHT_ARM_Z -> text = "right arm z"
                RIGHT_ELBOW_Y -> text = "right elbow"
                RIGHT_HIP_X -> text = "right hip x"
                RIGHT_HIP_Y -> text = "right hip y"
                RIGHT_HIP_Z -> text = "right hip z"
                RIGHT_KNEE_Y -> text = "right knee"
                RIGHT_SHOULDER_X -> text = "right shoulder horizontal"
                RIGHT_SHOULDER_Y -> text = "right shoulder vertical"
                UNKNOWN -> text = "unknown"
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