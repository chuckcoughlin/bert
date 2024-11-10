/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

/**
 * Well-known keys for the command property inside a request/response.
 * By design commands do not require access to the motor controller.
 */
enum class CommandType {
    // Command contained in a message request
    CREATE_ACTION,
    CREATE_FACE,
    CREATE_POSE,
    DELETE_USER_DATA,
    HALT,
    SHUTDOWN,
    SLEEP,
    WAKE,
    NONE;

    companion object {
        /**
         * Convert the Command enumeration to text that can be pronounced.
         * @param joint the enumeration
         * @return user-recognizable text
         */
        fun toText(command: CommandType): String {
            var text = ""
            when (command) {
                CREATE_ACTION -> text = "delete action"
                CREATE_FACE   -> text = "delete face"
                CREATE_POSE   -> text = "delete pose"
                DELETE_USER_DATA -> text = "delete action, pose or face"
                HALT     -> text = "halt"
                SHUTDOWN -> text = "shutdown"
                SLEEP    -> text = "sleep"
                WAKE     -> text = "wake"
                NONE     -> text = "none"
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
        /**
         * The enumeration function valueOf appears to always throw an exception.
         * This is the replacement. It is case-insensitive,
         */
        fun fromString(arg: String): CommandType {
            for (type in CommandType.values()) {
                if (type.name.equals(arg, true)) return type
            }
            return CommandType.NONE
        }
    }
}