/**
 * Based on examples from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
 */
package chuckcoughlin.bert.speech.process

import chuckcoughlin.bert.common.message.MessageBottle
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import java.util.logging.Logger

/**
 * As errors are detected, add their parameters to the dictionary.
 */
class SpeechErrorListener(bot: MessageBottle) : BaseErrorListener() {
    private val bottle: MessageBottle

    init {
        bottle = bot
    }

    override fun syntaxError(recognizer: Recognizer<*, *>?,offendingSymbol: Any?,
        line: Int, charPositionInLine: Int,msg: String?,e: RecognitionException? ) {

        recordError(offendingSymbol as Token?)
    }

    fun recordError(offendingToken: Token? ) {
        // Defer to the parser.
        if (bottle.error.isBlank()) {
            if (offendingToken != null) {
                val msg = java.lang.String.format(
                    "I didn't understand what came after %s",
                    offendingToken.getStartIndex(),
                    offendingToken.getText()
                )
                LOGGER.info(CLSS + msg)
                bottle.error = msg
            }
            else {
                val msg = "I don't understand"
                LOGGER.info(CLSS + msg)
                bottle.error = msg
            }
        }
    }

    companion object {
        private const val CLSS = "SpeechErrorListener: "
        private val LOGGER = Logger.getLogger(CLSS)
    }
}