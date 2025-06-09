/**
 * Copyright 2022-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bertspeak.common.MessageType
import java.util.Date

/**
 * Instances of this class are displayed in the MotorProperties table
 */
data class MotorData(val msg: String, val type: MessageType ) {
    val timestamp: Date
    val message:String
    val messageType: MessageType

    init {
        timestamp = Date()
        message = msg
        messageType = type
    }
}
