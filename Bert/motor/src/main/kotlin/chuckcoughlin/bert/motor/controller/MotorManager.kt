/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.motor.controller

import chuckcoughlin.bert.common.message.MessageBottle
/**
 * This interface is satisfied by the MotorGroupController and describes
 * the callbacks utilized by individual MotorControllers
 */
interface MotorManager {
    suspend fun handleAggregatedResponse(cname:String,response: MessageBottle):MessageBottle
    suspend fun handleSynthesizedResponse(response: MessageBottle):MessageBottle
    suspend fun handleSingleControllerResponse(response: MessageBottle):MessageBottle

    var controllerCount: Int
}