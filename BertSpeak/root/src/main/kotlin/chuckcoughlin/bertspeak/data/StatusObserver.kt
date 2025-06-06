/**
 * Copyright 2019-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

/**
 * Interface for entities which need to be informed about state
 * and other changes within the various managers. Recipients should
 * filter on DispatchData action.
 */
interface StatusObserver {
    /**
     * Allow only one observer of a given name. This is the observer key
     * @return the name of the observer
     */
    val name: String

    /**
     * Call this method after an observer newly registers. The
     * intention is to allow the observer to "catch-up" with the
     * state of the manager all at oncw.
     * @param list of StatusData that describe the current joint parameters
     */
    fun resetStatus(list: List<StatusData>)
    fun updateStatus(data: StatusData)
}