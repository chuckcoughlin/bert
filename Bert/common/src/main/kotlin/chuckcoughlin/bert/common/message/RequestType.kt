/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

/**
 * These are the recognized commands from command controller to launcher.
 */
enum class RequestType {
    COMMAND,                 // Execute a command may have one or no args
    EXECUTE_ACTION,          // Execute the poses in an action in sequence
    EXECUTE_POSE,            // Drive robot motors to pre-set values
    GET_APPENDAGE_LOCATION,  // x,y,z location of the named appendage, i.e. end effector
    GET_JOINT_LOCATION,      // x,y,z location of the center of the named joint
    GET_METRIC,              // A local property of the robot, e.g. name
    GET_MOTOR_PROPERTY,      // Current value of a motor property
    HANGUP,                  // Client has disconnected
    HEARTBEAT,               // An internal message on the TimerQueue
    INITIALIZE_JOINTS,       // Make sure that all joints are in "sane" positions
    JSON,                    // Message is in Json format for computer-computer.
    NOTIFICATION,            // Unsolicited message from server or parser
    PARTIAL,                 // Remainder of text has yet to arrive
    READY,                   // Synchronization message for internal controller
    RESET,                   // Reset synchronization for serial response and internal controller
    READ_MOTOR_PROPERTY,     // Read a single property for a motor or all motors and record internally
    SET_LIMB_PROPERTY,       // Torque or speed for motors in a limb
    SET_MOTOR_PROPERTY,      // For a particular motor
    NONE;

    companion object {
        /**
         * @return  a comma-separated list of the types in a single String.
         */
        fun names(): String {
            val names = StringBuffer()
            for( type in values() ) {
                names.append(type.name + ", ")
            }
            return names.substring(0, names.length - 2)
        }
    }
}