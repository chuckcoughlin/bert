/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

/**
 * Well-known keys for the properties map inside a request/response
 * Additional data values as appropriate are keyed with the JointProperty keys
 * Multiple properties are allowed within a given message.
 */
enum class PropertyType {
    // Well-known keys for the properties map inside a request/response
    APPENDAGE ,  // Request applies to this appendage, value is a Appendage
    COMMAND ,    // Value is a well-known command name, see below
    CONTROLLER_NAME,  // Message needs to be addressed to a specific controller
    ERROR ,           // Request resulted in an error, value is error text
    JOINT,       // Request applies to this joint, value is a Joint
    LIMB,        // Request applies to this limb, value is a Limb
    METRIC,      // Value is a MetricType
    POSE_NAME,        // Name of a pose, must exist in database
    PROPERTY_NAME,    // Value is a JointProperty. Subject of original request.
    TYPE,             // Type of request, a RequestType
    SOURCE,           // Original source of request, value is a HandlerType
    TEXT,             // End-user appropriate text result
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