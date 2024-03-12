/**
 * Copyright 2018-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * Recognized types of Dynamixel motors (minus dashes in name)
 */
enum class DynamixelType {
    AX12, MX28, MX64, UNDEFINED;

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
        /**
         * The enumeration function valueOf appears to always throw an exception.
         * This is the replacement. It is case-insensitive,
         */
        fun fromString(arg: String): DynamixelType {
            for (type in DynamixelType.values()) {
                if (type.name.equals(arg, true)) return type
            }
            return DynamixelType.UNDEFINED
        }
    }
}