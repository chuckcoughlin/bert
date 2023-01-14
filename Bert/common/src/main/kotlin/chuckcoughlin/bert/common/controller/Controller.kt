/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.controller

import chuckcoughlin.bert.common.message.MessageBottle
import kotlinx.coroutines.channels.Channel

/**
 * A common interface both for controllers that communicate with
 * the dispatcher (through channels) and also with external entities.
 * Channel naming is from the point of vie of the current class.
 * If there are no local or parent channels (e.g. in the case of the
 * Dispatcher), uninitialized channels will be created to ensure
 * non-null behavior.
 */
interface Controller {
    val localRequestChannel: Channel<MessageBottle>  // used internally and by child
    val localResponseChannel:Channel<MessageBottle>  // used internally and by child
    val parentRequestChannel: Channel<MessageBottle>  // from the parent controller
    val parentResponseChannel:Channel<MessageBottle>  // from the parent controller
    val controllerName: String

    suspend fun start()
    // Closes all resources including channels
    suspend fun stop()
}