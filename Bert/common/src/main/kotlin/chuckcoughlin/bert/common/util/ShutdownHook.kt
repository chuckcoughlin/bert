/**
 * Copyright 2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.util

import chuckcoughlin.bert.common.controller.Controller
import kotlinx.coroutines.runBlocking
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Register this class with the Runtime to cleanup sockets on a
 * SIGTERM, SIGINT, or SIGHUP. We do a hard shutdown.
 *
 * It appears as if the logging is in-effective here.
 */
class ShutdownHook
/**
 * Constructor: Create a new hook.
 */(private val controller: Controller) : Thread() {
    override fun run() {
        LOGGER.info(String.format("%s: shutting down %s...", CLSS, controller.controllerName))
        try {
            runBlocking {
                controller.stop()
            }
        }
        catch (ex: Exception) {
            LOGGER.log(Level.SEVERE, String.format("%s: ERROR (%s)", CLSS, ex.message), ex)
        }
        Runtime.getRuntime().halt(0)
    }

    companion object {
        private const val CLSS = "ShutdownHook"
        private val LOGGER = Logger.getLogger(CLSS)
    }
}