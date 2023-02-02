/**
 * Copyright 2019-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

/**
 * This is a marker for the Dispatcher to let it keep track of the controller
 * from which an individual message had come, so it can determine where it
 * needs to go.
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