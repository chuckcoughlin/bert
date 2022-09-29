/**
 * Based on examples from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
 */
package chuckcoughlin.bert.speech.process

import org.antlr.v4.runtime.BaseErrorListener
import java.util.logging.Logger

/**
 * As errors are detected, add their parameters to the dictionary.
 */
class SpeechErrorListener(bot: MessageBottle) : BaseErrorListener() {
    private val bottle: MessageBottle

    init {
        bottle = bot
    }

    fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int, charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        recordError(
            recognizer, offendingSymbol as Token?,
            line, charPositionInLine
        )
    }

    protected fun recordError(
        recognizer: Recognizer<*, *>?,
        offendingToken: Token?, line: Int,
        charPositionInLine: Int
    ) {
        // Defer to the parser.
        if (bottle.fetchError() == null) {
            if (offendingToken != null) {
                val msg = java.lang.String.format(
                    "I didn't understand what came after %s",
                    offendingToken.getStartIndex(),
                    offendingToken.getText()
                )
                LOGGER.info(CLSS + msg)
                bottle.assignError(msg)
            } else {
                val msg = "I don't understand"
                LOGGER.info(CLSS + msg)
                bottle.assignError(msg)
            }
        }
    }

    companion object {
        private const val CLSS = "SpeechErrorListener: "
        private val LOGGER = Logger.getLogger(CLSS)
    }
}