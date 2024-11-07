/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

/**
 * Define strings used in requests and responses between server and client.
 */
object BottleConstants {
    // Command names
    const val COMMAND_FREEZE = "freeze"
    const val COMMAND_HALT = "halt"
    const val COMMAND_RELAX = "relax"
    const val COMMAND_RESET = "reset"
    const val COMMAND_SHUTDOWN = "shutdown"
    const val COMMAND_SLEEP = "sleep"
    const val COMMAND_WAKE = "wake"

    // Controller names
    const val CONTROLLER_LOWER = "lower"
    const val CONTROLLER_UPPER = "upper"

    // Message from tablet
    const val HEADER_LENGTH = 4 // Includes semi-colon

    // Default values for some "empty" properties
    const val NO_ARG    = "No Argument"
    const val NO_CONTROLLER = "No Controller"
    const val NO_DELAY = 0L
    const val NO_ERROR = "No Error"
    const val NO_SOURCE = "No Source"

}