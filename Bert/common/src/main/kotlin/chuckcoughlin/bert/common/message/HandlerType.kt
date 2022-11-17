/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

/**
 * Recognized types of messsage handler. These are used to match specific
 * process instance with definitions in the configuration file.
 * The "launcher" is a server application and accesses all controllers.
 */
enum class HandlerType {
    BITBUCKET,
    COMMAND,
    DISPATCHER,
    INTERNAL,
    REQUEST,
    SERIAL,
    TABLET,
    TERMINAL,
    UNDEFINED;

    companion object {
        /**
         * @return  a comma-separated list of all handler types in a single String.
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