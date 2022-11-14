/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
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

    // Pose names (these are required to exist)
    const val POSE_HOME = "home"
    const val POSE_NORMAL_SPEED = "normal speed"

    // Message from tablet
    const val HEADER_LENGTH = 4 // Includes semi-colon

    // Nominal values for torque and speed in percent
    const val SPEED_NORMAL = 20
    const val TORQUE_NORMAL = 20

    // For values that are boolean. Use these strings for "values"
    const val ON_VALUE = "1"
    const val OFF_VALUE = "0"

    // Default values for some "empty" properties
    const val NO_CONTROLLER = "No Controller"
    const val NO_ERROR = "No Error"
    const val NO_POSE  = "No Pose"
    const val NO_SOURCE = "No Source"

}