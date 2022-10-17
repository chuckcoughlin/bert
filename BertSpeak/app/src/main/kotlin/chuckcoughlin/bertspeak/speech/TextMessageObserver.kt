/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.speech

/**
 * Interface for entities which need to be informed about new text messages
 * that are destined to be logged or enunciated.
 */
interface TextMessageObserver {
    /**
     * Allow only one observer of a given name.
     * @return the name of the observer
     */
    public val name: String

    /**
     * Call this method after an observer newly registers. The
     * manager allows the observer to "catch-up" with the
     * current state of the message list. The manager should be
     * retained and used to refresh the displayed list at will.
     */
    fun initialize()

    /**
     * Notify the observer that a new text message has been added
     * to the manager's list. Only appropriate client types are
     * included.
     * @param msg the new message
     */
    fun update(msg: TextMessage)
}
