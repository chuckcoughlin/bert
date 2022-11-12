/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

/**
 * Well-known keys for the command property inside a request/response
 */
enum class CommandType {
    // Command contained in a message request
    FREEZE ,
    HALT,
    RELAX ,
    RESET,
    SHUTDOWN,
    SLEEP,
    WAKE;

    companion object {
        /**
         * Convert the Command enumeration to text that can be pronounced.
         * @param joint the enumeration
         * @return user-recognizable text
         */
        fun toText(command: CommandType): String {
            var text = ""
            when (command) {
                FREEZE   -> text = "freeze"
                HALT     -> text = "halt"
                RELAX    -> text = "relax"
                RESET    -> text = "reset"
                SHUTDOWN -> text = "shutdown"
                SLEEP    -> text = "sleep"
                WAKE     -> text = "wake"
            }
            return text
        }
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