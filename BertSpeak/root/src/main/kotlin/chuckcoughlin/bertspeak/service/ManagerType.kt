/**
 * (c) 2022-2024  Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

/**
 * The following are the communication controllers embedded in this application.
 * HEARING handles speech to text
 * SOCKET handles read/write to the wifi network and robot server
 * SPEECH handles text to speech


 */
enum class ManagerType {
    FACES, HEARING, GEOMETRY, SOCKET, SPEECH, STATUS, TEXT, NONE;

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
