/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

/**
 * These quantities are attributes of the robot as a whole
 * and are not derived from motor parameters.
 */
enum class MetricType {
    AGE, CADENCE, CYCLECOUNT, CYCLETIME, DUTYCYCLE,
    HEIGHT, MITTENS, NAME, UNDEFINED;

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