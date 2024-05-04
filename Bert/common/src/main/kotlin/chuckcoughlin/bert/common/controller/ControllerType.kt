/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.controller

/**
 * This is a marker for the Dispatcher to let it keep track of the controller
 * from which an individual message had come, so it can determine where it
 * needs to go. The type is defined in the XML configuration file.
 */
enum class ControllerType {
    BITBUCKET,
    COMMAND,
    DISPATCHER,
    INTERNAL,
    MOTOR,      // Serial
    MOTORGROUP, // Controls the collection of motors
    SOCKET,
    TABLET,
    TERMINAL,
    UNDEFINED;

    companion object {
        /**
         * @return  a comma-separated list of all controller types in a single String.
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
        fun fromString(arg: String): ControllerType {
            for (type in ControllerType.values()) {
                if (type.name.equals(arg, true)) return type
            }
            return ControllerType.UNDEFINED
        }
    }
}