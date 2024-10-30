/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * This class should be identical in both Linux and Android worlds.
 */
package chuckcoughlin.bert.common.message

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
    APPENDAGE_NAMES,  // List of appendages               MutableList<String>
    FACIAL_DETAILS,   // Facial identification features   FacialDetails
    FACE_DIRECTION,   // Angular direction of view        FaceDirection
    FACE_NAMES,       // Names df people whom we know     MutableList<String>
    JOINT_NAMES,      // List of joint names              MutableList<String>
    JOINT_IDS,        // Motor id for each joint        MutableList<JointAttribute>
    JOINT_TYPES,      // Motor type for each joint        MutableList<JointAttribute>
    JOINT_POSITIONS,  // Current positions of all nmotors  MutableList<JointPosition>
    LIMB_NAMES,       // List of limb names               MutableList<String>
    MOTOR_DYNAMIC_PROPERTIES, // Names of dynamic motor properties  MutableList<String>
    MOTOR_STATIC_PROPERTIES,  // Names of static motor properties    MutableList<String>
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