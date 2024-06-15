/**
 * Copyright 2023-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bertspeak.service.ManagerState
import chuckcoughlin.bertspeak.service.ManagerType

/**
 * Instances of this class are displayed in the log.
 */
data class StatusData(val act:String) {
    val action = act
    var type = ManagerType.NONE
    var state= ManagerState.NONE
    // Miscellaneous data, action and type specific
    val payload = mutableMapOf<String,Any>()

    init {

    }
}
