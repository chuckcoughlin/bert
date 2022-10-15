/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * Configures a java.util.logging Logger to use our log directory.
 */
package chuckcoughlin.bert.common.util

import chuckcoughlin.bert.common.PathConstants
import java.io.*
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter

/**
 * This class is a singleton that features a method to configure the root logger
 * and, presumably, all it progeny.
 */
class LoggerUtility
/**
 * Constructor is private per Singleton pattern.
 */
private constructor() {
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
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Configure the loggers to output INFO to std out, do not send to a log file.
     * @param root core name for the log files
     */
    fun configureTestLogger(rootName: String?) {
        val root = Logger.getLogger("")
        val handlers = root.handlers
        for (h in handlers) {
            h.level = Level.INFO // Display info and worse on console
            h.formatter = BertFormatter()
            if (h is FileHandler) root.removeHandler(h)
        }
    }

    inner class BertFormatter : Formatter() {
        private val LINE_SEPARATOR = System.getProperty("line.separator")
        private val dateFormatter: SimpleDateFormat

        init {
            dateFormatter = SimpleDateFormat(Companion.DATE_PATTERN)
        }

        override fun format(record: LogRecord): String {
            val sb = StringBuilder()
            sb.append(dateFormatter.format(Date(record.millis)))
                .append(String.format("%-6s", record.level.localizedName))
                .append(": ")
                .append(formatMessage(record))
                .append(LINE_SEPARATOR)
            if (record.thrown != null) {
                try {
                    val sw = StringWriter()
                    val pw = PrintWriter(sw)
                    record.thrown.printStackTrace(pw)
                    pw.close()
                    sb.append(sw.toString())
                } catch (ex: Exception) {
                }
            }
            return sb.toString()
        }

        companion object {
            private const val DATE_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS "
        }
    }

    companion object {
        const val MAX_BYTES = 100000 // Max bytes in a log file
        const val MAX_FILES = 3 // Max log files before overwriting

        /**
         * Static method to create and/or fetch the single instance.
         */
        var instance: LoggerUtility? = null
            get() {
                if (field == null) {
                    synchronized(LoggerUtility::class.java) { field = LoggerUtility() }
                }
                return field
            }
            private set
    }
}