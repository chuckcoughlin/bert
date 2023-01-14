/**
 * Copyright 2022,2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.term.controller

import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.speech.process.MessageTranslator
import chuckcoughlin.bert.speech.process.StatementParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.logging.Logger

/**
 * The StdIO controller handles input/output to/from stdin and sdout for interactive
 * command-line operation. The typed commands and text responses are exactly the same
 * as the spoken interface with the Command application.
 *
 * requestChannel = Stdin
 * responseChannel= Stdout
 *
 * @param prmpt prompt displayed for user entry
 */
class StdioController(prmpt: String,parent: Controller) : Controller {
    private val parser: StatementParser
    private val translator: MessageTranslator
    private val prompt: String
    val dispatcher = parent
    var running = false
    override var parentRequestChannel = dispatcher.localRequestChannel   // from stdIn
    override var parentResponseChannel = dispatcher.localResponseChannel // routed to stdOut

    override suspend fun start() {
        running = true
        val br = BufferedReader(InputStreamReader(System.`in`))
        runBlocking<Unit> {
            launch {
                Dispatchers.IO
                while(running) {
                    select<Unit> {
                        parentResponseChannel.onReceive() {
                            displayMessage(it)   // stdOut
                        }
                        /**
                         * Read from stdin, blocked. Use ANTLR to convert text into requests.
                         * Forward requests to the Terminal launcher.
                         */
                        async {
                            handleUserInput(br)
                        }
                    }
                }
            }
        }
    }

    override suspend fun stop() {
        if( running ) {
            localRequestChannel.close()
            localResponseChannel.close()
            dispatcher.stop()
            running = false
        }
    }

    /**
     * The message is expected to carry understandable text, an error
     * message or a value that can be formatted into user-ready text.
     * Send directly to stdOut
     * @param response
     */
    fun displayMessage(msg: MessageBottle) {
        val text: String = translator.messageToText(msg)
        println(text)
        print(prompt)
    }

    /**
     * Read directly from stdin
     */
    suspend fun handleUserInput(br:BufferedReader) {
        val input = br.readLine()
        if( input!=null) {
            val text = input!!
            System.out.println(prompt)
            if (!text.isEmpty()) {
                LOGGER.info(String.format("%s:parsing %s", CLSS, text))
                val request = parser.parseStatement(text)
                if( !request.error.isEmpty() ||
                     request.type.equals(RequestType.NOTIFICATION) ) {
                    displayMessage(request)   // Take care of locally to stdOut
                }
                else {
                    parentRequestChannel.send(request)
                }
            }
        }
    }

    private val CLSS = "StdioController"
    protected val LOGGER = Logger.getLogger(CLSS)
    override val controllerName = CLSS
    override val localRequestChannel = Channel<MessageBottle>()  // Not used
    override val localResponseChannel = Channel<MessageBottle>() // Not used

    init {
        prompt = prmpt
        parser = StatementParser()
        translator = MessageTranslator()
    }
}