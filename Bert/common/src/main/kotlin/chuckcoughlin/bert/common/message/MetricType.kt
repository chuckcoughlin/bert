/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

/**
 * These quantities are attributes of the robot as a whole.
 */
enum class MetricType {
    AGE, APPENDAGES, CADENCE, CYCLECOUNT, CYCLETIME, DUTYCYCLE,
    HEIGHT, JOINTS, LIMBS, MITTENS, NAME, UNDEFINED;

    companion object {
        /**
         * @return  a comma-separated list of status types in a single String.
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
        fun fromString(arg: String): MetricType {
            for (type in MetricType.values()) {
                if (type.name.equals(arg, true)) return type
            }
            return MetricType.UNDEFINED
        }
    }
}