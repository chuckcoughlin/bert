/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

/**
 * These quantities are attributes of the robot as a whole.
 */
enum class MetricType {
    AGE, CADENCE, CYCLECOUNT, CYCLETIME, DUTYCYCLE, HEIGHT, MITTENS,  // why
    NAME;

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
    }
}