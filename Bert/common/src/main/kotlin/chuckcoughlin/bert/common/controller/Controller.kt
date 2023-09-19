/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.controller

/**
 * A common interface both for controllers that communicate with
 * the dispatcher (through channels) and also with external entities.
 * Channel naming is from the point of view of the current class.
 */
interface Controller {
    val controllerName: String
    val controllerType: ControllerType

    suspend fun execute()
    // Closes all resources including channels
    suspend fun shutdown()
}