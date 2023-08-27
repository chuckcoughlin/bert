/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * Configures a java.util.logging Logger to use our log directory.
 */
package chuckcoughlin.bert.common.util

import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Formatter
import java.util.logging.LogRecord


/**
 * Format a logger record
 */
class BertFormatter() : Formatter() {

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
            }
            catch (ex: Exception) {
            }
        }
        return sb.toString()
    }

     fun printStackTrace() {
        kotlin.runCatching {
            //any code that can throw an exception
            throw Exception()
        }.onFailure {
            //print stack trace
            it.printStackTrace()
        }
    }

    val DATE_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS "
    val LINE_SEPARATOR = System.getProperty("line.separator")
    val dateFormatter: SimpleDateFormat

    init {
        dateFormatter = SimpleDateFormat(DATE_PATTERN)
    }
}