/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common

/**
 * Define strings used in requests and responses between server and client.
 */
object BottleConstants {
    // Well-known keys for the properties map inside a request/response
    // Additional data values as appropriate are keyed with the JointProperty keys
    const val APPENDAGE_NAME = "appendage" // Request applies to this appendage, value is a Appendage
    const val COMMAND_NAME = "command" // Value is a well-known command name, see below
    const val CONTROLLER_NAME = "controller" // Message needs to be addressed to a specific controller
    const val ERROR = "error" // Request resulted in an error, value is error text
    const val JOINT_NAME = "joint" // Request applies to this joint, value is a Joint
    const val LIMB_NAME = "limb" // Request applies to this limb, value is a Limb
    const val METRIC_NAME = "metric" // Value is a MetricType
    const val POSE_NAME = "pose" // Name of a pose, must exist in database
    const val PROPERTY_NAME = "property" // Value is a JointProperty. Subject of original request.
    const val TYPE = "type" // Type of request, a RequestType
    const val SOURCE = "source" // Original source of request, value is a HandlerType
    const val TEXT = "text" // End-user appropriate text result

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

    // Default responses
    const val NO_ERROR = "No Error"
    const val NO_SOURCE = "No Source"

}