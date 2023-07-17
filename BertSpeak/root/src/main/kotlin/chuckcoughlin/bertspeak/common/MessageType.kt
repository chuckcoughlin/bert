/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * This class should be identical in both Linux and Android worlds.
 */
package chuckcoughlin.bertspeak.common

/**
 * These are the recognized header strings for messages between the tablet
 * and robot. The header is separated from the body of the message by a ':'.
 */
enum class MessageType {
    ANS,  // Reply from the robot. The tablet should "speak" the contents
    LOG,  // A system message meant to be appended to the "Log" panel (or syslog).
    MSG,  // Request or query from the tablet, plain english  
    ROW,  // Pipe-delimited data to be added to the most recent table          
    TBL // Define a table for the table panel. Pipe-delimited title and column headings
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