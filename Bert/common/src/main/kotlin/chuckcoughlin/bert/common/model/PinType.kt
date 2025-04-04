/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * These are the possible types of "LinkPins" or
 * connection locations on a link.
 */
enum class PinType {
    END_EFFECTOR, ORIGIN, REVOLUTE;

    companion object {
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