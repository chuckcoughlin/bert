/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * This class should be identical in both Linux and Android worlds.
 */
package chuckcoughlin.bert.common.message

/**
 * This is a key known to both tablet and robot that appears in a Json
 * message and indicates the class represented by the Json code. The message
 * format is like:
 *   JSN:JOINT_NAMES  ... json list of joint names, e.g.
 *
 * The Json type is followed by a space before the remainder of the message
 */
enum class JsonType {
    FACE // Facial identity information
    ;

    companion object {
        /**
         * @return  a comma-separated list of the types in a single String.
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