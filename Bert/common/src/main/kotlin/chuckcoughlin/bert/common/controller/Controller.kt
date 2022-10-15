/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.controller

import chuckcoughlin.bert.common.message.MessageBottle

/**
 * A common interface for controllers owned by application instances.
 */
interface Controller {
    fun receiveRequest(request: MessageBottle?)
    fun receiveResponse(response: MessageBottle?)
    fun start()
    fun stop()
}