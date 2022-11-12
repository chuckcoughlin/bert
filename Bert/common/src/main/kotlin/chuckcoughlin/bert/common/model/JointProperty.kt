/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * These are properties of each stepper motor.
 */
enum class JointProperty {
    ID,
    MAXIMUMANGLE,
    MINIMUMANGLE,
    MOTORTYPE,
    OFFSET,
    ORIENTATION,
    POSITION,
    SPEED,
    STATE,
    TEMPERATURE,
    TORQUE,
    VOLTAGE,
    UNRECOGNIZED;

    companion object {
        /**
         * @return  a comma-separated list of all property types in a single String.
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