/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * This class should be identical in both Linux and Android worlds.
 */
package chuckcoughlin.bertspeak.data

/**
 * This is a key known to both tablet and robot that appears in a Json
 * message and indicates the class represented by the Json code. The message
 * format is like:
 *   JSN:JOINT_NAMES  json list of joint names, e.g.
 *
 * The Json type is followed by a space before the remainder of the message
 * in Json format. Refer to the commentary below for the data object that
 * corresponsd to each type
 */
enum class JsonType {
    ACTION_NAMES,     // List of actions                     MutableList<String>
    BONE_NAMES,       // List of link names (bones)          MutableList<String>
    EXTREMITY_LOCATION, // x,y,z coordinates of an extremity ExtremityLocation
    EXTREMITY_NAMES,  // List of extremeties              MutableList<String>
    FACIAL_DETAILS,   // Facial identification features   FacialDetails
    FACE_DIRECTION,   // Angular direction of view        FaceDirection
    FACE_NAMES,       // Names df people whom we know     MutableList<String>
    JOINT_IDS,        // Motor id for each joint          MutableList<JointAttribute>
    JOINT_NAMES,      // List of joint names              MutableList<String>
    JOINT_OFFSETS,    // Motor offset for each joint      MutableList<JointAttribute>
    JOINT_ORIENTATIONS, // Motor orientation for each joint MutableList<JointAttribute>
    JOINT_POSITIONS,  // Current positions of all motors  MutableList<JointValue>
    JOINT_SPEEDS,     // Motor speeds for each joint      MutableList<JointValue>
    JOINT_STATES,     // Motor states for each joint      MutableList<JointAttribute>
    JOINT_TORQUES,     // Motor torques for each joint    MutableList<JointValue>
    JOINT_TEMPERATURES,// Motor temps for each joint      MutableList<JointValue>
    JOINT_TYPES,      // Motor type for each joint        MutableList<JointAttribute>
    JOINT_VOLTAGES,   // Motor volts for each joint       MutableList<JointValue>
    LIMB_LOCATIONS,   // x,y,z coordinates for each joint MutableList<JointLocation>
    LIMB_NAMES,       // List of limb names               MutableList<String>
    MOTOR_DYNAMIC_PROPERTIES, // Names of dynamic motor properties  MutableList<String>
    MOTOR_STATIC_PROPERTIES,  // Names of static motor properties    MutableList<String>
    MOTOR_GOALS,      // Position goals for a specific joint         MutableList<JointPropertyValue>
    MOTOR_LIMITS,     // Motor limits for a specific joint           MutableList<JointPropertyValue>
    POSE_DETAILS,     // Details df named pose                       MutableList<PoseDetail>
    POSE_NAMES,       // Names df defined poses                      MutableList<String>
    UNDEFINED
    ;

    companion object {
        /**
         * @return  a comma-separated list of the types in a single String.
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
        fun fromString(arg: String): JsonType {
            for (type in JsonType.values()) {
                if (type.name.equals(arg, true)) return type
            }
            return JsonType.UNDEFINED
        }
    }
}
