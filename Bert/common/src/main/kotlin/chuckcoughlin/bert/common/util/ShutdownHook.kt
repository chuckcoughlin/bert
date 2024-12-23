/**
 * Copyright 2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.util

import chuckcoughlin.bert.common.controller.Controller
import kotlinx.coroutines.runBlocking

/**
 * Register this class with the Runtime to cleanup sockets on a
 * SIGTERM, SIGINT, or SIGHUP. We do a hard shutdown. This is the
 * proper way to shut down the application.
 *
 * It appears as if the logging is in-effective here.
 */
class ShutdownHook(private val controller: Controller) : Thread() {
    override fun run() {
        println(String.format("\n%s: shutting down %s...", CLSS, controller.controllerName))
        runBlocking {
            try {
                controller.shutdown()
            }
            catch (e: Exception) {
                println(String.format("\n%s: ERROR in shutdown %s", CLSS, e.localizedMessage))
                e.printStackTrace()
            }

            println(String.format("\n%s: shutdown complete.", CLSS))
            Runtime.getRuntime().halt(0)
        }
    }
    val CLSS = "ShutdownHook"
}