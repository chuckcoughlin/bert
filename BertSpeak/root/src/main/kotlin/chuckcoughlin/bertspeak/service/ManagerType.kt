/**
 * (c) 2022-2024  Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

/**
 * These are the communication controllers embedded in this application.
 */
enum class ManagerType {
    ANNUNCIATOR,BLUETOOTH, DISCOVERY, GEOMETRY, SOCKET, SPEECH, STATUS, TEXT, NONE;

    companion object {
        /**
         * @return  a comma-separated list of all facilities in a single String.
         */
        fun names(): String {
            val names = StringBuffer()
            for (state in values()) {
                names.append(state.name + ", ")
            }
            return names.substring(0, names.length - 2)
        }
    }
}
