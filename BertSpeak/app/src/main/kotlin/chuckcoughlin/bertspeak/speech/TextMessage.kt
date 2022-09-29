/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.speech

import chuckcoughlin.bertspeak.common.*
import java.util.*

/**
 * Instances of this class are displayed in the log.
 */
class TextMessage(private val type: MessageType, private val msg: String) {
    private val timestamp: Date
    fun getMessage(): String {
        return msg
    }

    fun getTimestamp(): Date {
        return timestamp
    }

    fun getMessageType(): MessageType {
        return type
    }

    init {
        timestamp = Date()
    }
}