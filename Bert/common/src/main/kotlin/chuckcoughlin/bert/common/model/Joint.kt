/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.util.TextUtility
import java.util.logging.Logger

/**
 * These are the canonical names for the joints of the humanoid.
 */
enum class Joint {
    ABS_X, ABS_Y, ABS_Z, BUST_X, BUST_Y, NECK_Y, NECK_Z,
    LEFT_ANKLE_Y, LEFT_SHOULDER_Z, LEFT_ELBOW_Y, LEFT_HIP_X, LEFT_HIP_Y, LEFT_HIP_Z,
    LEFT_KNEE_Y, LEFT_SHOULDER_X, LEFT_SHOULDER_Y,
    RIGHT_ANKLE_Y, RIGHT_SHOULDER_Z, RIGHT_ELBOW_Y, RIGHT_HIP_X, RIGHT_HIP_Y, RIGHT_HIP_Z,
    RIGHT_KNEE_Y, RIGHT_SHOULDER_X, RIGHT_SHOULDER_Y, IMU, NONE;

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
                BUST_X -> text = "chest horizontal"
                BUST_Y -> text = "chest vertical"
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
                NONE -> text = "unknown"
            }
            return text
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

        /** @return  a comma-separated list of common names for the joints.
        */
        fun nameList(): String {
            val list = mutableListOf<String>()
            for (joint in values()) {
                if( joint==Joint.NONE) continue
                list.add(Joint.toText(joint))
            }
            return TextUtility.createTextForSpeakingFromList(list)
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