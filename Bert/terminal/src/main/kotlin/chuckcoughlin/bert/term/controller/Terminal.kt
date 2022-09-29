/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.term.controller

import bert.share.common.BottleConstants
import java.nio.file.Paths
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.logging.Logger

/**
 * "Terminal" is an application that allows interaction from the command line
 * to command and interrogate the robot. Typed entries are the same as
 * those given to the "headless" application, "Command", in spoken form.
 *
 * The application acts as the intermediary between a StdioController and
 * SocketController communicating with the Dispatcher.
 */
class Terminal(m: RobotTerminalModel) : Thread(), MessageHandler {
    private val model: RobotTerminalModel
    private var socketController: SocketController? = null
    private val busy: Condition
    private var currentRequest: MessageBottle? = null
    private var ignoring: Boolean
    private val lock: Lock
    private var stdioController: StdioController? = null
    private val messageTranslator: MessageTranslator

    init {
        model = m
        ignoring = false
        lock = ReentrantLock()
        busy = lock.newCondition()
        messageTranslator = MessageTranslator()
    }

    /**
     * This application contains a stdio stdioController and a client socket stdioController
     */
    fun createControllers() {
        val prompt: String = model.getProperty(ConfigurationConstants.PROPERTY_PROMPT, "bert:")
        stdioController = StdioController(this, prompt)
        val hostName: String = model.getProperty(ConfigurationConstants.PROPERTY_HOSTNAME, "localhost")
        val sockets: Map<String, Int> = model.getSockets()
        val walker = sockets.keys.iterator()
        val key = walker.next()
        val port = sockets[key]!!
        socketController = SocketController(this, HandlerType.TERMINAL.name(), hostName, port)
    }

    val controllerName: String
        get() = model.getProperty(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, "terminal")

    /**
     * Loop forever reading from the terminal and forwarding the resulting requests
     * via socket to the server (launcher). We accept its responses and forward to the stdio stdioController.
     *
     * Note that the locks guarantee that the currentRequest global cannot be modified between the signal and wait.
     */
    override fun run() {
        try {
            while (true) {
                lock.lock()
                try {
                    busy.await()
                    if (currentRequest == null) break
                    if (currentRequest.fetchRequestType().equals(RequestType.COMMAND) &&
                        BottleConstants.COMMAND_HALT.equalsIgnoreCase(
                            currentRequest.getProperties().get(BottleConstants.COMMAND_NAME)
                        )
                    ) {
                        socketController.receiveRequest(currentRequest) // halt the dispatcher as well
                        sleep(EXIT_WAIT_INTERVAL)
                        break
                    } else if (isLocalRequest(currentRequest)) {
                        // Handle local request -create response
                        val response: MessageBottle = handleLocalRequest(currentRequest)
                        if (response != null) handleResponse(response)
                    } else if (!ignoring) {
                        socketController.receiveRequest(currentRequest)
                    }
                } catch (ie: InterruptedException) {
                } finally {
                    lock.unlock()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            shutdown()
        }
        Database.getInstance().shutdown()
        LOGGER.warning(String.format("%s: exiting...", CLSS))
        System.exit(0)
    }

    fun startup() {
        socketController.start()
        stdioController!!.start()
    }

    fun shutdown() {
        socketController.stop()
        stdioController!!.stop()
    }

    /**
     * We've gotten a request (must be from a different thread than our main loop). Signal
     * to release the lock and send along to the launcher.
     */
    fun handleRequest(request: MessageBottle?) {
        lock.lock()
        try {
            currentRequest = request
            busy.signal()
        } finally {
            lock.unlock()
        }
    }

    /**
     * We've gotten a response. Send it to our Stdio stdioController
     * which ultimately writes it to stdout.
     */
    fun handleResponse(response: MessageBottle?) {
        stdioController!!.receiveResponse(response)
    }

    // We handle the command to sleep and awake immediately.
    private fun handleLocalRequest(request: MessageBottle): MessageBottle {
        if (request.fetchRequestType().equals(RequestType.COMMAND)) {
            val command: String = request.getProperty(BottleConstants.COMMAND_NAME, "NONE")
            //LOGGER.warning(String.format("%s.handleLocalRequest: command=%s",CLSS,command));
            if (command.equals(BottleConstants.COMMAND_SLEEP, ignoreCase = true)) {
                ignoring = true
            } else if (command.equals(BottleConstants.COMMAND_WAKE, ignoreCase = true)) {
                ignoring = false
            } else {
                val msg = String.format("I don't recognize command %s", command)
                request.assignError(msg)
            }
        }
        request.assignText(messageTranslator.randomAcknowledgement())
        return request
    }

    // Local requests are those that can be handled immediately without forwarding to the dispatcher.
    private fun isLocalRequest(request: MessageBottle): Boolean {
        if (request.fetchRequestType().equals(RequestType.COMMAND)) {
            val properties: Map<String, String> = request.getProperties()
            val cmd = properties[BottleConstants.COMMAND_NAME]
            if (cmd.equals(BottleConstants.COMMAND_SLEEP, ignoreCase = true) ||
                cmd.equals(BottleConstants.COMMAND_WAKE, ignoreCase = true)
            ) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val CLSS = "Terminal"
        private const val USAGE = "Usage: terminal <robot_root>"
        private const val EXIT_WAIT_INTERVAL: Long = 1000
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
            LoggerUtility.getInstance().configureRootLogger(LOG_ROOT)
            val model = RobotTerminalModel(PathConstants.CONFIG_PATH)
            model.populate()
            Database.getInstance().startup(PathConstants.DB_PATH)
            val runner = Terminal(model)
            runner.createControllers()
            Runtime.getRuntime().addShutdownHook(ShutdownHook(runner))
            runner.startup()
            runner.start()
        }
    }
}