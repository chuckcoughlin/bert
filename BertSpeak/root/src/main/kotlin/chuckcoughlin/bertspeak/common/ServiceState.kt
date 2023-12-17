/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * This class should be identical in both Linux and Android worlds.
 */
package chuckcoughlin.bertspeak.common

/**
 * These are the possble states for a foreground service:'.
 */
enum class ServiceState {
    RUNNING,        // Operable
    UNINITIALIZED,  // Prior to receiving the startup intent or after a destroy()
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
