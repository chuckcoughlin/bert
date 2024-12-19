/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bertspeak.data.JsonType
import chuckcoughlin.bertspeak.common.MessageType
import java.util.Date
/**
 * This class holds a JSON string representing a class
 * that is determined by the JsonType.
 */
data class JsonData(val data: String, val type: JsonType ) {
    val timestamp: Date
    val json:String
    val jsonType: JsonType

    init {
        timestamp = Date()
        json = data
        jsonType = type
    }
}
