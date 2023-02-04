/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.share.controller

import java.util.*

/**
 * Implementation of a change listener for socket state
 */
interface SocketStateChangeListener : EventListener {
    /**
     * @param event contains information about the source and the new state.
     */
    fun stateChanged(event: SocketStateChangeEvent?)
}