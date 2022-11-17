/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.term.controller

import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.message.HandlerType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.MessageHandler
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.speech.process.MessageTranslator
import chuckcoughlin.bert.speech.process.StatementParser
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.logging.Logger

/**
 * The StdIO controller handles input/output to/from stdin and sdout for interactive
 * command-line operation. The typed commands and text responses are exactly the same
 * as the spoken interface with the Command application.
 */
class StdioController(launcher: MessageHandler, text: String) : Controller {
    private val LOGGER = Logger.getLogger(CLSS)
    private val dispatcher: MessageHandler
    private var runner: Thread? = null
    private val parser: StatementParser
    private val translator: MessageTranslator
    private val prompt: String



    override fun start() {
        val rdr = StdinReader(dispatcher)
        runner = Thread(rdr)
        runner!!.start()
    }

    override fun stop() {
        if (runner != null) {
            runner!!.interrupt()
            runner = null
        }
    }

    override fun receiveRequest(request: MessageBottle) {
        dispatcher.handleRequest(request)
    }

    /**
     * The response is expected to carry understandable text, an error
     * message or a value that can be formatted into understandable text.
     * @param response
     */
    override fun receiveResponse(response: MessageBottle) {
        val text: String = translator.messageToText(response)
        println(text)
        print(prompt)
    }

    /**
     * Loop forever reading from the stdin. Use ANTLR to convert text into requests.
     * Forward requests to the Terminal launcher.
     */
    inner class StdinReader(disp: MessageHandler?) : Runnable {
        /**
         * Forever ...
         * 1) Read user entry from std in
         * 2) Convert to a request and send to Terminal message handler
         */
        override fun run() {
            var br: BufferedReader? = null
            try {
                br = BufferedReader(InputStreamReader(System.`in`))
                while (!Thread.currentThread().isInterrupted) {
                    print(prompt)
                    val input: String = br.readLine()
                    if (input.isEmpty()) continue else {
                        LOGGER.info(java.lang.String.format("%s parsing: %s", HandlerType.TERMINAL.name, input))
                        val request: MessageBottle = parser.parseStatement(input)
                        request.handler = HandlerType.TERMINAL
                        if( !request.error.isBlank()|| request.type.equals(RequestType.NOTIFICATION)) {
                            receiveResponse(request) // Handle locally/immediately
                        }
                        else {
                            receiveRequest(request)
                        }
                    }
                }
            }
            catch (ioe: IOException) {
                ioe.printStackTrace()
            }
            catch (ex: Exception) {
                ex.printStackTrace()
            }
            finally {
                if (br != null) {
                    try {
                        br.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    companion object {
        private const val CLSS = "StdioController"
    }

    /**
     * @param launcher the parent application
     * @param text prompt displayed for user entry
     */
    init {
        dispatcher = launcher
        prompt = text
        parser = StatementParser()
        translator = MessageTranslator()
    }
}