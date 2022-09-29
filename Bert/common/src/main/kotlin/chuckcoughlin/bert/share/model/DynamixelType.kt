/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.share.model

/**
 * Recognized types of Dynamixel motors (minus dashes in name)
 */
enum class DynamixelType {
    AX12, MX28, MX64;

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