/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * Define strings found in the configuration file
 */
object ConfigurationConstants {
    // Keys for standard controller names
    const val CONTROLLER_DISPATCHER = "Dispatcher"

    // Keys for properties found in the main configuration file
    const val PROPERTY_CADENCE = "cadence"
    const val PROPERTY_CONTROLLER_NAME = "controller"
    const val PROPERTY_DEVICE = "device"
    const val PROPERTY_HOSTNAME = "hostname"
    const val PROPERTY_PORT = "port"
    const val PROPERTY_PROMPT = "prompt"
    const val PROPERTY_ROBOT_NAME = "name"
    const val PROPERTY_SOCKET = "socket"
    const val PROPERTY_NONE = "none"

    // Flags set on the command-line
    const val PROPERTY_USE_NETWORK   = "network"
    const val PROPERTY_USE_SERIAL    = "serial"
    const val PROPERTY_USE_TERMINAL  = "terminal"

    // Command-line keys to set debug logging
    const val DEBUG_COMMAND    = "cmd"
    const val DEBUG_CONFIGURATION = "cfg"
    const val DEBUG_DISPATCHER = "dsp"
    const val DEBUG_GROUP      = "grp"
    const val DEBUG_INTERNAL   = "int"
    const val DEBUG_MOTOR      = "mtr"
    const val DEBUG_MESSAGE    = "msg"
    const val DEBUG_SERIAL     = "ser"
    const val DEBUG_SOLVER     = "slv"
    const val DEBUG_DATABASE   = "sql"
    const val DEBUG_TERMINAL   = "ter"

    // Pose names (these are required to exist)
    const val POSE_HOME = "home"
    const val POSE_HOME_INDEX = 1.0

    // Minimum spacing between serial writes ~ msec
    const val MIN_SERIAL_WRITE_INTERVAL = 100L   // 50 was too short
    const val LONG_TME_AGO = 10000L              // 10 seconds

    // Nominal values for torque and speed
    const val SPEED_NORMAL = 200.0        // Was 300
    const val TORQUE_MAX = 95.0          // ~percent

    // For values that are boolean. Use these strings for "values"
    const val ON_VALUE  = 1.0
    const val OFF_VALUE = 0.0

    // Error valu
    const val NO_CONTROLLER  = "Not Defined"
    const val NO_DEVICE      = "No Device"
    const val NO_NAME        = "No Name"
    const val NO_PATH        = "No Path"
    const val NO_PORT        = "No Port"
    const val NO_VALUE       = "No Value"
}