/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.share.util

import bert.share.message.MessageHandler
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
 */(private val msghandler: MessageHandler) : Thread() {
    override fun run() {
        LOGGER.info(String.format("%s: shutting down %s...", CLSS, msghandler.controllerName))
        try {
            msghandler.shutdown()
        } catch (ex: Exception) {
            LOGGER.log(Level.SEVERE, String.format("%s: ERROR (%s)", CLSS, ex.message), ex)
        }
        Runtime.getRuntime().halt(0)
    }

    companion object {
        private const val CLSS = "ShutdownHook"
        private val LOGGER = Logger.getLogger(CLSS)
    }
}