/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * Configures a java.util.logging Logger to use our log directory.
 */
package chuckcoughlin.bert.common.util

import chuckcoughlin.bert.common.PathConstants
import java.io.*
import java.nio.file.Paths
import java.util.*
import java.util.logging.*


/**
 * This class contains static functions to configure the root logger
 * and, presumably, all its progeny.
 */
object LoggerUtility {

    /**
     * @param root core name for the log files
     */
    fun configureRootLogger(rootName: String) {
        val root = Logger.getLogger("")
        val handlers = root.handlers
        for (h in handlers) {
            h.level = Level.WARNING // Display warnings and worse on console
            h.formatter = BertFormatter()
            if (h is FileHandler) root.removeHandler(h)
        }
        val fh: FileHandler
        try {
            // Configure the logger with handler and formatter
            val pattern = Paths.get(PathConstants.LOG_DIR.toString(), "$rootName.log")
            fh = FileHandler(pattern.toString(), MAX_BYTES, MAX_FILES, true)
            fh.level = Level.INFO
            root.addHandler(fh)
            fh.formatter = BertFormatter()
        }
        catch (e: SecurityException) {
            e.printStackTrace()
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Configure the loggers to output INFO to std out, do not send to a log file.
     * @param root core name for the log files
     */
    fun configureTestLogger(rootName: String?) {
        val root = Logger.getLogger(rootName)
        val handlers = root.handlers
        for (h in handlers) {
            h.level = Level.INFO // Display info and worse on console
            h.formatter = BertFormatter()
            if (h is FileHandler) root.removeHandler(h)
        }
    }



    const val MAX_BYTES = 100000 // Max bytes in a log file
    const val MAX_FILES = 3 // Max log files before overwriting
}