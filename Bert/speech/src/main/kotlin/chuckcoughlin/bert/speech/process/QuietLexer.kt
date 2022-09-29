/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material,
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose.
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
 */
package chuckcoughlin.bert.speech.process

import org.antlr.v4.runtime.CharStream

/**
 * This class is a failed attempt to quiet the lexer complaints about
 * token recognition errors whenever we hit whitespace.
 */
class QuietLexer(input: CharStream?) : SpeechSyntaxLexer(input) {
    /**
     * Since we have elected to ignore whitespace, recover() is called
     * to skip over an space that is encountered. Let it recover silently.
     *
     * line 1:13 token recognition error at: ' '
     */
    fun recover(e: LexerNoViableAltException?) {
        super.recover(e)
        // log.info(TAG+"AFTER recover LexerNoViableAltException");
    }

    fun recover(e: RecognitionException?) {
        super.recover(e)
    }

    companion object {
        private const val CLSS = "QuietLexer"
    }
}