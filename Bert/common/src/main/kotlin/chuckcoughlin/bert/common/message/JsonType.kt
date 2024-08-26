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
 *   JSN:POSENM  ... json list of known poses, e.g.
 *
 * All Json types are 8 characters
 */
enum class JsonType {
    APPENDAGE_NAMES,    // Appendage names
    JOINT_NAMES,        // Joint names
    LIMB_NAMES,         // Limb names
    MOTOR_PROP_NAMES,   // Motor property names
    MOTOR_CFGS,    // Motor configuration list
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