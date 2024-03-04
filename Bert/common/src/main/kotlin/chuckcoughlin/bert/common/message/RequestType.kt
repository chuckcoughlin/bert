/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

/**
 * These are the recognized commands from command controller to launcher.
 */
enum class RequestType {
    COMMAND,                 // Execute a no-arg command, usually non-motor-related
    GET_APPENDAGE_LOCATION,  // x,y,z location of the named appendage
    GET_GOALS,               // The current joint target settings
    GET_JOINT_LOCATION,      // x,y,z location of the center of the named joint
    GET_LIMITS,              // The EEPROM-resident joint limits
    GET_METRIC,              // A local property of the robot, e.g. name
    GET_MOTOR_PROPERTY,      // Current value of a motor property
    GET_POSE,
    HEARTBEAT,               // An internal message on the TimerQueue
    INITIALIZE_JOINTS,       // Make sure that all joints are in "sane" positions
    LIST_MOTOR_PROPERTIES,   // List available properties for a motor (static/dynamic) - JSON
    LIST_MOTOR_PROPERTY,     // List a single property for all motors  - JSON
    MAP_POSE,                // Associate a command to a pose
    NOTIFICATION,            // Unsolicited message from server or parser
    PARTIAL,                 // Remainder of text has yet to arrive
    PLAY_STEP, RECORD_STEP, SAVE_POSE,  // Save the current pose to the database
    READ_MOTOR_PROPERTY,     // Read a single property for all motors and record internally
    SET_LIMB_PROPERTY,       // Torque or speed for motors in a limb
    SET_MOTOR_PROPERTY,      // For a particular motor
    SET_POSE,                // Position robot in the named pose
    SET_STATE,               // A global configuration, like ignoring
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