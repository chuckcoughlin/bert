/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.controller

import chuckcoughlin.bert.common.message.MessageBottle

/**
 * A speciality controller that provides a callback for asynchronous
 * handling of a message from a subcontroller..
 */
interface MessageController:Controller {
    suspend fun dispatchMessage(msg: MessageBottle)
}