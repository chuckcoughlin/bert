/**
 * (c) 2022  Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

/**
 * This enumeration class represents the permissible states of tiered facilities
 * within the voice service.
 */
enum class FacilityState {
    IDLE, WAITING, ACTIVE, ERROR;

    companion object {
        /**
         * @return  a comma-separated list of all state values in a single String.
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