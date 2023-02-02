/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.motor.controller

import chuckcoughlin.bert.common.message.MessageBottle

/**
 * Wrap a request/response message while processing within the MotorController.
 * The ultimate purpose is to attach a serial message response count to the
 * request so that we can determine when the response is complete.
 * @author chuckc
 */
class MessageWrapper(msg: MessageBottle) {
    val message: MessageBottle

    /**
     * The response count indicates the number of serial responses
     * yet expected. When this count is zero, the response is
     * ready to be sent along to the group controller.
     * @return the remaining count
     */
    var responseCount: Int

    init {
        message = msg
        responseCount = 1
    }

    fun decrementResponseCount() {
        responseCount = responseCount - 1
    }
}