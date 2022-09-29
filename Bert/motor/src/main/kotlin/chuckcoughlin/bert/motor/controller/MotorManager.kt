/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.motor.controller

import bert.share.message.MessageBottle

/**
 * This interface is satisfied by the MotorGroupController and describes
 * the callbacks utilized by individual MotorContrtollers
 */
interface MotorManager {
    fun handleAggregatedResponse(response: MessageBottle?)
    fun handleSynthesizedResponse(response: MessageBottle)
    fun handleSingleControllerResponse(response: MessageBottle?)
    val controllerCount: Int
}