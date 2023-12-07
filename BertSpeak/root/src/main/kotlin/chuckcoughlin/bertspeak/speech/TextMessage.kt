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
    val timestamp: Date
    val message:String
    val messageType: MessageType

    init {
        timestamp = Date()
        message = msg
        messageType = type
    }
}
