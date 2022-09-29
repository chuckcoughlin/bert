/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.share.message

/**
 * This interface describes the central processes that launch controllers
 * and manage communications between them.
 * The request methods are synchronous; the handler processes one request at a time.
 * The response method is expected to be a callback used on completion of the
 * current request.
 */
interface MessageHandler {
    /**
     * Controllers handle communication with peripheral entities. Instances
     * specific to this application are created here.
     */
    fun createControllers()
    val controllerName: String?
    fun run()
    fun handleRequest(request: MessageBottle?)
    fun handleResponse(response: MessageBottle?)
    fun startup()
    fun shutdown()
}