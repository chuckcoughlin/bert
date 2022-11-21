/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.term.controller

import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.PathConstants
import chuckcoughlin.bert.common.message.CommandType
import chuckcoughlin.bert.common.message.HandlerType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.MessageHandler
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.util.LoggerUtility
import chuckcoughlin.bert.common.util.ShutdownHook
import chuckcoughlin.bert.speech.process.MessageTranslator
import chuckcoughlin.bert.sql.db.Database

import chuckcoughlin.bert.term.model.RobotTerminalModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.logging.Logger

/**
 * "Terminal" is a static class that allows interaction from the command line
 * to command and interrogate the robot. Typed entries are the same as
 * those given to the "headless" application, "Command", in spoken form.
 *
 * Input is StdIn and output is Stdout. Communication channels are
 * created by the dispatcher and supplied to the "process" function.
 */
class Terminal(configPath: Path,receiveChannel: ReceiveChannel<MessageBottle>,sendChannel: SendChannel<MessageBottle>) : MessageHandler {
    private val messageTranslator: MessageTranslator
    private val model: RobotTerminalModel
    var lazy stdioController: StdioController
    val scope = MainScope() // Uses Dispatchers.Main
    val toStdOut = receiveChannel
    val fromStdIn = sendChannel
    var running:Boolean


    fun CoroutineScope.process(getStdIn:ReceiveChannel<MessageBottle>,
                                putStdOut:SendChannel<MessageBottle> ) = launch {
        waitUntil(running)
        while(running) {

        }
    }

    /**
     * This application contains a stdio stdioController and a client socket stdioController
     */
    override fun createControllers() {
        val prompt: String = model.getProperty(ConfigurationConstants.PROPERTY_PROMPT, "bert:")
        stdioController = StdioController(this, prompt)
    }

    override val controllerName: String
        get() = model.getProperty(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, "terminal")



    // Does this ever complete?
    override fun startup() {
        super.startup()
        running = true
        scope.launch {
           while( running )  {
               select<Unit> {
                   toStdOut.onReceive {
                       msg = toStdOut.receive()
                       if (msg.type.equals(RequestType.COMMAND) &&
                           msg.command.equals(CommandType.COMMAND_HALT ) ) {
                           shutdown()
                       }
                       else if(isLocalRequest(msg)) {
                           // Handle local request -create response
                           val response = handleLocalRequest(msg)
                           if( !response.type.equals(RequestType.NONE)) handleResponse(response)
                       }
                       else if (!ignoring) {
                           fromStdIn.onSend(msg)
                       }
                   }

                   fromStdIn.onSend(msg) {

                   }
               }
           }
        }
    }

    // Shutdown the entire application right here?
    override fun shutdown() {
        runnng = false
        scope.cancel()
        super.shutdown()
        Database.shutdown()
        LOGGER.warning(String.format("%s: exiting...", CLSS))
        System.exit(0)
    }

    /**
     * We've gotten a request (must be from a different thread than our main loop). Signal
     * to release the lock and send along to the launcher.
     */
    fun handleRequest(request: MessageBottle) {
        lock.lock()
        try {
            currentRequest = request
            busy.signal()
        }
        finally {
            lock.unlock()
        }
    }

    /**
     * We've gotten a response. Send it to our Stdio stdioController
     * which ultimately writes it to stdout.
     */
    override fun handleResponse(response: MessageBottle) {
        stdioController!!.receiveResponse(response)
    }

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
        request.text = messageTranslator.randomAcknowledgement()
        return request
    }

    // Local requests are those that can be handled immediately without forwarding to the dispatcher.
    private fun isLocalRequest(request: MessageBottle): Boolean {
        if (request.type.equals(RequestType.COMMAND)) {
            val cmd = request.command
            if (cmd.equals(CommandType.SLEEP) ||
                cmd.equals(CommandType.WAKE )) {

                return true
            }
        }
        return false
    }

    companion object {
        private const val CLSS = "Terminal"
        private const val USAGE = "Usage: terminal <robot_root>"
        private val LOGGER = Logger.getLogger(CLSS)
        private val LOG_ROOT = CLSS.lowercase(Locale.getDefault())

        /**
         * Entry point for the application that allows direct user input through
         * stdio. The argument specifies a directory that is the root of the various
         * robot configuration, code and devices.
         *
         * Usage: terminal <bert_root>
         *
         * @param args command-line arguments. Only one matters.
        </bert_root> */
        @JvmStatic
        fun main(args: Array<String>) {

            // Make sure there is command-line argument
            if (args.size < 1) {
                println(USAGE)
                System.exit(1)
            }

            // Analyze command-line argument to obtain the file path to BERT_HOME.
            val arg = args[0]
            val path = Paths.get(arg)
            PathConstants.setHome(path)
            // Setup logging to use only a file appender to our logging directory
            LoggerUtility.configureRootLogger(LOG_ROOT)
            val model = RobotTerminalModel(PathConstants.CONFIG_PATH)
            model.populate()
            Database.startup(PathConstants.DB_PATH)
            val runner = Terminal(model)
            runner.createControllers()
            Runtime.getRuntime().addShutdownHook(ShutdownHook(runner))
            runner.startup()
            runner.start()
        }
    }



    init {
        messageTranslator = MessageTranslator()
        model = RobotTerminalModel(configPath)
        running = false
    }
}