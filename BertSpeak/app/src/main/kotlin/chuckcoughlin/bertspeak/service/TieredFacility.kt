/**
 * (c) 2022  Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

/**
 * These are the facilities supported by the DispatchService. These
 * are ordered in that each is dependent on the previous.
 */
enum class TieredFacility {
    BLUETOOTH, SOCKET, VOICE;

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