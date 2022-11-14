/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * These are properties of a stepper motor that are modifiable
 * at runtime. These all have numeric values.
 */
enum class JointDynamicProperty {
    POSITION,
    SPEED,
    STATE,
    TEMPERATURE,
    TORQUE,
    VOLTAGE,
    NONE;

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