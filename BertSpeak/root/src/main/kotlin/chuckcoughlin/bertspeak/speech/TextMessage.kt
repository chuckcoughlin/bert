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
data class TextMessage(val msg: String, val type: MessageType ) {
    private val timestamp: Date
    val message:String
        get() =  msg

    val messageType: MessageType
        get() = type

    fun getTimestamp(): Date {return timestamp }

    init {
        timestamp = Date()
    }
}
