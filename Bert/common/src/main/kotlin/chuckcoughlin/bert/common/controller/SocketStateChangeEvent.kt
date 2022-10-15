/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.controller

import java.util.*

/**
 * This is asa custom event class that conveys information aout the state of a socket.
 *
 */
class SocketStateChangeEvent
/**
 * Constructor. Value is a simple object (not null,not a QualifiedValue)
 * @param source the event originator
 * @param name the name of the connection that originated the event
 * @param state the new state taken from one of the defined jointValues in this class
 */(source: Any?, val name: String?, val state: String?) : EventObject(source) {

    companion object {
        private const val serialVersionUID = -7642269391076595297L

        // Recognized state names
        const val READY = "ready"
    }
}