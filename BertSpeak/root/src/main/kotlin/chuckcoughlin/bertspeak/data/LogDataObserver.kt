/**
 * Copyright 2023-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

/**
 * Interface for entities which need to be informed about new text messages
 * that are destined to be logged or enunciated.
 */
interface LogDataObserver {
    /**
     * Allow only one observer of a given name.
     * @return the name of the observer
     */
    val name: String

    /**
     * Call this method after an observer newly registers. This
     * allows the observer to "catch-up" with the
     * current state of the message list. The manager should be
     * retained and used to refresh the displayed list at will.
     */
    fun resetText(list: List<LogData>)

    /**
     * Notify the observer that a new text message has been added
     * to the manager's list. Only appropriate client types are
     * included.
     * @param msg the new message
     */
    fun updateText(msg: LogData)
}
