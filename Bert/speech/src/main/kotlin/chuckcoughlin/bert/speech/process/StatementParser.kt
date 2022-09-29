/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.process

import org.antlr.v4.runtime.CharStreams

/**
 * Parse spoken text using ANTLR classes. A context dictionary is passed
 * between invocations of the parser.
 */
class StatementParser {
    private val context: HashMap<SharedKey, Any?>

    /**
     * Constructor provides parameters specific to the robot. The
     * shared dictionary is intended for communication between the
     * invocations of the translator.
     */
    init {
        context = HashMap()
        initialize()
    }

    /**
     * Initialize the shared dictionary
     */
    private fun initialize() {
        context[SharedKey.ASLEEP] = "false"
        context[SharedKey.APPENDAGE] = Appendage.UNKNOWN
        context[SharedKey.AXIS] = "x"
        context[SharedKey.CONTROLLER] = ""
        context[SharedKey.JOINT] = Joint.UNKNOWN
        context[SharedKey.LIMB] = Limb.UNKNOWN
        context[SharedKey.POSE] = "home"
        context[SharedKey.SIDE] = "right"
        context[SharedKey.IT] = SharedKey.JOINT
    }

    fun setSharedProperty(key: SharedKey, value: Any?) {
        context[key] = value
    }

    /**
     * This is the method that parses a statement - one line of text. It uses the visitor pattern to
     * traverse the parse tree and generate the returned statement prototype. This method parses one line.
     *
     * Any partial text is saved off and prepended to the next string.
     *
     * @param cmd user-entered english string
     * @return a request bottle to be sent to the server
     */
    @Throws(Exception::class)
    fun parseStatement(text: String?): MessageBottle {
        var text = text
        val bottle = MessageBottle()
        if (text != null) {
            if (context[SharedKey.PARTIAL] != null) {
                text = String.format("%s %s", context[SharedKey.PARTIAL], text)
                context.remove(SharedKey.PARTIAL)
            }
            val bais = ByteArrayInputStream(text.toByteArray())
            val stream: CodePointCharStream = CharStreams.fromString(text)
            val lexer: SpeechSyntaxLexer = QuietLexer(stream)
            lexer.removeErrorListeners() // Quiet lexer gripes
            val tokens = CommonTokenStream(lexer)
            val parser = SpeechSyntaxParser(tokens)
            parser.removeErrorListeners() // remove default error listener
            parser.addErrorListener(SpeechErrorListener(bottle))
            parser.setErrorHandler(SpeechErrorStrategy(bottle))
            val tree: ParseTree = parser.line() // Start with a line
            val visitor = StatementTranslator(bottle, context)
            visitor.visit(tree)
            if (bottle.fetchRequestType().equals(RequestType.PARTIAL)) {
                context[SharedKey.PARTIAL] = text
            }
        } else {
            bottle.assignError("Empty")
        }
        return bottle
    }
}