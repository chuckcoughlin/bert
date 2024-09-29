/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.process

/**
 * These are properties in the shared dictionary that can be shared from
 * instance to instance.
 */
enum class SharedKey {
    APPENDAGE,
    ASLEEP,  // Robot ignores requests,
    AXIS,
    FACE,    // Recognized user
    JOINT,
    LIMB,
    PARTIAL,  // Text of an incomplete request
    POSE,     // Current pose
    SIDE,
    IT,       // Datatype of "it"
    SPEED
    ;

    companion object {
        /**
         * @return  a comma-separated list keys in a single String.
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