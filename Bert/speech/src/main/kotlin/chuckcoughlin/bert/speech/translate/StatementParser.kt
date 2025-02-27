/**
 * Copyright 2020-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.translate

import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.syntax.SpeechSyntaxLexer
import chuckcoughlin.bert.syntax.SpeechSyntaxParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CodePointCharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import java.util.logging.Logger

/**
 * Parse spoken text using ANTLR classes. A shared dictionary is passed
 * between invocations of the parser.
 */
class StatementParser {
    private val dictionary: MutableMap<SharedKey, Any>

    /**
     * Initialize the shared dictionary.
     */
    private fun initialize() {
        dictionary[SharedKey.ASLEEP] = "false"
        dictionary[SharedKey.APPENDAGE] = Appendage.NONE
        dictionary[SharedKey.AXIS] = "x"
        dictionary[SharedKey.JOINT] = Joint.NONE
        dictionary[SharedKey.LIMB] = Limb.NONE
        dictionary[SharedKey.POSE] = "home"
        dictionary[SharedKey.SIDE] = "right"
        dictionary[SharedKey.SPEED] = "normal speed"
        dictionary[SharedKey.IT] = SharedKey.JOINT
    }

    fun setSharedProperty(key: SharedKey, value: Any) {
        dictionary[key] = value
    }

    /**
     * This is the method that parses a statement - one line of text. It uses the visitor pattern to
     * traverse the parse tree and generate the returned statement prototype. This method parses one line.
     * Any partial text is saved off and prepended to the next string.
     *
     * @param cmd user-entered english string
     * @return a request bottle to be sent to the server
     */
    @Throws(Exception::class)
    fun parseStatement(txt: String): MessageBottle {
        var text = txt
        val bottle = MessageBottle(RequestType.NONE)
        if ( !text.isBlank() ) {
            if (dictionary[SharedKey.PARTIAL] != null) {
                text = String.format("%s %s", dictionary[SharedKey.PARTIAL], text)
                dictionary.remove(SharedKey.PARTIAL)
            }
            val stream: CodePointCharStream = CharStreams.fromString(text)
            val lexer: SpeechSyntaxLexer = QuietLexer(stream)
            lexer.removeErrorListeners() // Quiet lexer gripes
            val tokens = CommonTokenStream(lexer)
            val parser = SpeechSyntaxParser(tokens)
            parser.removeErrorListeners() // remove default error listener
            parser.addErrorListener(SpeechErrorListener(bottle))
            parser.setErrorHandler(SpeechErrorStrategy(bottle))
            val tree: ParseTree = parser.line() // Start with a line
            val visitor = StatementTranslator(bottle, dictionary)
            visitor.visit(tree)
            if (bottle.type.equals(RequestType.PARTIAL)) {
                dictionary[SharedKey.PARTIAL] = text
            }
        }
        else {
            bottle.error = "Empty"
        }
        if(DEBUG && !bottle.error.equals(BottleConstants.NO_ERROR)) {
            LOGGER.info(String.format("%s.parseStatement: %s ERROR %s",CLSS,txt,bottle.error))
        }
        return bottle
    }

    private val CLSS = "StatementParser"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)

    /**
     * Constructor provides parameters specific to the robot. The
     * shared dictionary is intended for communication between the
     * invocations of the translator.
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_COMMAND)
        dictionary = mutableMapOf<SharedKey,Any>()
        initialize()
    }
}