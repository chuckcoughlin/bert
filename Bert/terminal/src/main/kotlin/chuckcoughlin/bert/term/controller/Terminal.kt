/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.term.controller

import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.message.CommandType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.speech.process.MessageTranslator
import chuckcoughlin.bert.speech.process.StatementParser
import chuckcoughlin.bert.sql.db.Database
import chuckcoughlin.bert.term.model.RobotTerminalModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.*
import java.util.logging.Logger

/**
 * "Terminal" handles direct interaction from the user to command and
 * interrogate the robot. Typed entries are the same as given to the "Command"
 * controller in spoken form. Requests originate from stdIn and responses
 * sent to stdOut.
 *
 * Dispatcher.localResponseChannel  - messages that need to be displayed to the user
 * Terminal.localResponseChannel    - not used (stdOut)
 *
 * Dispatcher.localRequestChannel    - commands from the user
 * Terminal.localRequestChannel      - not used (stdIn)
 */
class Terminal(configPath: Path,parent: Controller) : Controller {
    private val parser: StatementParser
    private val translator: MessageTranslator
    private val model: RobotTerminalModel
    private val dispatcher = parent
    override var parentRequestChannel = dispatcher.localRequestChannel
    override var parentResponseChannel = dispatcher.localResponseChannel
    private var prompt:String

    val scope = MainScope() // Uses Dispatchers.Main
    var ignoring : Boolean
    var running:Boolean

    /**
     * When running this controller processes messages between the Dispatcher
     * and StdioController. A few messages are intercepted that cause a
     * quick shutdown. These are responses
     */
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
            running = false
            scope.cancel()
            Database.shutdown()
            LOGGER.warning(String.format("%s: exiting...", CLSS))
            System.exit(0)
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
            val text = input
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




    // Shutdown the entire application right here?



    // We handle the command to sleep and awake immediately.
    private fun handleLocalRequest(request: MessageBottle): MessageBottle {
        if (request.type.equals(RequestType.COMMAND)) {
            val command: CommandType = request.command
            //LOGGER.warning(String.format("%s.handleLocalRequest: command=%s",CLSS,command));
            if( command.equals(CommandType.SLEEP) ){
                ignoring = true
            }
            else if( command.equals(CommandType.WAKE)) {
                ignoring = false
            }
            else {
                val msg = String.format("I don't recognize command %s", command)
                request.error = msg
            }
        }
        request.text = translator.randomAcknowledgement()
        return request
    }

    private val CLSS = "Terminal"
    private val USAGE = "Usage: terminal <robot_root>"
    private val LOGGER = Logger.getLogger(CLSS)
    private val PROMPT = "Bert:"
    override var controllerName = CLSS
    override val localRequestChannel = Channel<MessageBottle>()  // From Stdin
    override val localResponseChannel = Channel<MessageBottle>() // To st

    init {
        parser = StatementParser()
        translator = MessageTranslator()
        model = RobotTerminalModel(configPath)
        controllerName = model.getProperty(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, CLSS)
        prompt = model.getProperty(ConfigurationConstants.PROPERTY_PROMPT, PROMPT)
        running = false
        ignoring = false
    }
}