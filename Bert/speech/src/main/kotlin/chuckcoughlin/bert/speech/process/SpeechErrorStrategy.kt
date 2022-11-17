/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
 */
package chuckcoughlin.bert.speech.process

import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import org.antlr.v4.runtime.DefaultErrorStrategy
import org.antlr.v4.runtime.InputMismatchException
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import java.util.logging.Logger

/** Instead of recovering from exceptions, log the information to
 * a logfile.
 */
class SpeechErrorStrategy(bot: MessageBottle) : DefaultErrorStrategy() {
    private val bottle: MessageBottle

    // Phases showing total lack of understanding ...
    private val phrases = arrayOf(
        "I don't understand",
        "Are you talking to me",
        "I do not comprehend"
    )

    init {
        bottle = bot
    }

    /**
     * This appears to be a top-level view of things
     */
    override fun recover(recognizer: Parser, e: RecognitionException?) {
        super.recover(recognizer, e)
        //LOGGER.warning(CLSS+": RECOVER");
        //recordError(recognizer,e);  // Moved to reportError() override
    }

    /** Make sure we don't attempt to recover inline; if the parser
     * successfully recovers, it won't throw an exception.
     */
    override fun recoverInline(recognizer: Parser): Token {
        // LOGGER.warning(CLSS+": RECOVER-INLINE");
        recordError(recognizer, InputMismatchException(recognizer))
        return super.recoverInline(recognizer)
    }

    /**
     * This appears to be a top-level view of things...
     */
    override fun reportError(recognizer: Parser, e: RecognitionException) {
        //LOGGER.warning(CLSS+":reportError ...");
        recordError(recognizer, e)
    }

    /** Make sure we don't attempt to recover from problems in sub-rules.  */
    override fun sync(recognizer: Parser) {}
    protected fun recordError(recognizer: Recognizer<*, *>?, re: RecognitionException) {
        // In each case the expected tokens are an expression. Don't bother to list
        val offender: Token = re.getOffendingToken()
        var msg: String = ""
        if (offender != null && offender.getText() != null && !offender.getText().isEmpty()) {
            msg = String.format("I don't understand the word \"%s\"", offender.getText())
        }
        else if (offender.getText() != null && offender.getText().startsWith("<EOF>")) {  // EOF
            bottle.type = RequestType.PARTIAL
        }
        else {  // Don't understand
            val rand = Math.random()
            val index = (rand * phrases.size).toInt()
            msg = phrases[index]
        }
        LOGGER.info(String.format("WARNING: %s: %s", CLSS, msg))
        bottle.error = msg
    }

    companion object {
        private const val CLSS = "SpeechErrorStrategy: "
        private val LOGGER = Logger.getLogger(CLSS)
    }
}