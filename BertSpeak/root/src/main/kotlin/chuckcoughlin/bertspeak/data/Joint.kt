/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import java.util.logging.Logger

/**
 * These are the canonical names for the joints of the humanoid.
 */
enum class Joint {
    ABS_X, ABS_Y, ABS_Z,CHEST_X, CHEST_Y, NECK_Y, NECK_Z,
    LEFT_ANKLE_Y, LEFT_SHOULDER_Z, LEFT_ELBOW_Y, LEFT_HIP_X, LEFT_HIP_Y, LEFT_HIP_Z,
    LEFT_KNEE_Y, LEFT_SHOULDER_X, LEFT_SHOULDER_Y,
    RIGHT_ANKLE_Y, RIGHT_SHOULDER_Z, RIGHT_ELBOW_Y, RIGHT_HIP_X, RIGHT_HIP_Y, RIGHT_HIP_Z,
    RIGHT_KNEE_Y, RIGHT_SHOULDER_X, RIGHT_SHOULDER_Y, IMU,
    LEFT_EAR, LEFT_EYE, LEFT_FINGER, LEFT_HEEL, LEFT_TOE, NOSE,
    RIGHT_EAR, RIGHT_EYE, RIGHT_FINGER, RIGHT_HEEL, RIGHT_TOE,NONE;

    companion object {
        private val CLSS = "Joint"
        private val LOGGER = Logger.getLogger(CLSS)
        /**
         * Convert the Joint enumeration to text that can be pronounced.
         * @param joint the enumeration
         * @return user-recognizable text
         */
        fun toText(joint: Joint): String {
            val text:String
            when (joint) {
                ABS_X -> text = "abdomen x"
                ABS_Y -> text = "abdomen y"
                ABS_Z -> text = "abdomen z"
                CHEST_X -> text = "chest horizontal"
                CHEST_Y -> text = "chest vertical"
                IMU -> text = "IMU"
                NECK_Y -> text = "neck y"
                NECK_Z -> text = "neck z"
                LEFT_ANKLE_Y -> text = "left ankle"
                LEFT_ELBOW_Y -> text = "left elbow"
                LEFT_HIP_X -> text = "left hip x"
                LEFT_HIP_Y -> text = "left hip y"
                LEFT_HIP_Z -> text = "left hip z"
                LEFT_KNEE_Y -> text = "left knee"
                LEFT_SHOULDER_X -> text = "left shoulder x"
                LEFT_SHOULDER_Y -> text = "left shoulder y"
                LEFT_SHOULDER_Z -> text = "left shoulder z"
                RIGHT_ANKLE_Y -> text = "right ankle"
                RIGHT_ELBOW_Y -> text = "right elbow"
                RIGHT_HIP_X -> text = "right hip x"
                RIGHT_HIP_Y -> text = "right hip y"
                RIGHT_HIP_Z -> text = "right hip z"
                RIGHT_KNEE_Y -> text = "right knee"
                RIGHT_SHOULDER_X -> text = "right shoulder x"
                RIGHT_SHOULDER_Y -> text = "right shoulder y"
                RIGHT_SHOULDER_Z -> text = "right shoulder z"
                // End effectors
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
         * @param joint the enumeration
         * @return true if the joint refers to an end-effector
         */
        fun isEndEffector(joint: Joint): Boolean {
            val ans:Boolean
            when (joint) {
                ABS_X -> ans = false
                ABS_Y -> ans = false
                ABS_Z -> ans = false
                CHEST_X -> ans = false
                CHEST_Y -> ans = false
                IMU -> ans = false
                NECK_Y -> ans = false
                NECK_Z -> ans = false
                LEFT_ANKLE_Y -> ans = false
                LEFT_ELBOW_Y -> ans = false
                LEFT_HIP_X -> ans = false
                LEFT_HIP_Y -> ans = false
                LEFT_HIP_Z -> ans = false
                LEFT_KNEE_Y -> ans = false
                LEFT_SHOULDER_X -> ans = false
                LEFT_SHOULDER_Y -> ans = false
                LEFT_SHOULDER_Z -> ans = false
                RIGHT_ANKLE_Y -> ans = false
                RIGHT_ELBOW_Y -> ans = false
                RIGHT_HIP_X -> ans = false
                RIGHT_HIP_Y -> ans = false
                RIGHT_HIP_Z -> ans = false
                RIGHT_KNEE_Y -> ans = false
                RIGHT_SHOULDER_X -> ans = false
                RIGHT_SHOULDER_Y -> ans = false
                RIGHT_SHOULDER_Z -> ans = false
                // End effectors
                LEFT_EAR -> ans = false
                LEFT_EYE -> ans = false
                LEFT_FINGER -> ans = false
                LEFT_HEEL -> ans = false
                LEFT_TOE -> ans = false
                NOSE -> ans = false
                RIGHT_EAR -> ans = false
                RIGHT_EYE -> ans = false
                RIGHT_FINGER -> ans = false
                RIGHT_HEEL -> ans = false
                RIGHT_TOE -> ans = false
                NONE -> ans = false
            }
            return ans
        }

        /**
         * @param joint the enumeration
         * @return true if the joint is the IMU, the root of any chain
         */
        fun isRoot(joint:Joint):Boolean {
            if(joint==IMU) return true
            else return false
        }

        /** @return  a comma-separated string of all joints
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
         * This is the replacement and is case insensitive.
         */
        fun fromString(arg: String): Joint {
            for (j:Joint in values()) {
                if (j.name.equals(arg, true)) return j
            }
            LOGGER.warning(String.format("%s.fromString: no match for %s",CLSS,arg ))
            return NONE
        }
    }
}
