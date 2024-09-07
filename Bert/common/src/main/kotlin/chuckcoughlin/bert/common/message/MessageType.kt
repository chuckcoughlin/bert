/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * This class should be identical in both Linux and Android worlds.
 */
package chuckcoughlin.bert.common.message

/**
 * These are the recognized header strings for messages between the tablet
 * and robot. The header is separated from the body of the message by a ':'.
 * The message is terminated with a line feed or carriage return.
 */
enum class MessageType {
    ANS,  // Reply from the robot. The tablet should "speak" the contents
    JSN,  // Arbitrary data from the robot in JSON format
    LOG,  // A system message meant to be appended to the log file.
    MSG   // Request or query from the tablet, plain english
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
    }
}