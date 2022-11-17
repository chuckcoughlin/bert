/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * These are properties that define a stepper motor.
 * These are defined in the URDF file and are not
 * generally modifiable.
 */
enum class JointDefinitionProperty {
    ID,
    MOTORTYPE,
    OFFSET,
    ORIENTATION;

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