/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.command.controller


import chuckcoughlin.bert.command.model.RobotCommandModel
import chuckcoughlin.bert.common.BottleConstants
import chuckcoughlin.bert.common.PathConstants
import chuckcoughlin.bert.common.controller.SocketController
import chuckcoughlin.bert.common.message.HandlerType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.MessageHandler
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.util.LoggerUtility
import chuckcoughlin.bert.common.util.ShutdownHook
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger

/**
 * This is the main client class that handles spoken commands and forwards
 * them on to the central launcher. It also handles database actions
 * involving playback and record.
 */
class Command(m: RobotCommandModel) : Thread(), MessageHandler {
    private val model: RobotCommandModel
    private var tabletController: BluetoothController? = null
    private val messageTranslator: MessageTranslator
    private var dispatchController: SocketController? = null
    private val busy: Condition
    private var currentRequest: MessageBottle? = null
    private val lock: Lock
    private var ignoring: Boolean

    init {
        model = m
        lock = ReentrantLock()
        busy = lock.newCondition()
        ignoring = false
        messageTranslator = MessageTranslator()
    }

    /**
     * This application routes requests/responses between the Dispatcher and "blueserverd" daemon. Both
     * destinations involve socket controllers.
     */
    fun createControllers() {
        tabletController = BluetoothController(this, model.getBlueserverPort())
        val hostName: String = model.getProperty(ConfigurationConstants.PROPERTY_HOSTNAME, "localhost")
        val sockets: Map<String, Int> = model.getSockets()
        val walker = sockets.keys.iterator()
        val key = walker.next()
        val port = sockets[key]!!
        dispatchController = SocketController(this, HandlerType.COMMAND.name(), hostName, port)
    }

    val controllerName: String
        get() = model.getProperty(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, "command")

    /**
     * Loop forever reading from the bluetooth daemon (representing the tablet) and forwarding the resulting requests
     * via socket to the server (launcher). We accept its responses and forward back to the tablet.
     * Communication with the tablet consists of simple strings, plus a 4-character header.
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
                        dispatchController.receiveRequest(currentRequest) // halt the dispatcher as well
                        sleep(EXIT_WAIT_INTERVAL)
                        break
                    } else if (isLocalRequest(currentRequest)) {
                        // Handle local request -create response
                        val response: MessageBottle = handleLocalRequest(currentRequest)
                        if (response != null) handleResponse(response)
                    } else if (!ignoring) {
                        dispatchController.receiveRequest(currentRequest)
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
        System.exit(0)
    }

    fun startup() {
        dispatchController.start()
        tabletController!!.start()
    }

    fun shutdown() {
        dispatchController.stop()
        tabletController.stop()
    }

    /**
     * We've gotten a request (presumably from the BluetoothController). Signal
     * to release the lock to send along to the dispatcher.
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
     * We've gotten a response. Send it to our BluetoothController
     * which ultimately writes it to the Android tablet.
     */
    fun handleResponse(response: MessageBottle?) {
        tabletController!!.receiveResponse(response)
    }

    // We handle the command to sleep and awake immediately.
    private fun handleLocalRequest(request: MessageBottle): MessageBottle {
        if (request.fetchRequestType().equals(RequestType.COMMAND)) {
            val command: String = request.getProperty(BottleConstants.COMMAND_NAME, "NONE")
            LOGGER.warning(String.format("%s.handleLocalRequest: command=%s", CLSS, command))
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
        private const val CLSS = "Command"
        private const val USAGE = "Usage: command <config-file>"
        private val LOGGER = Logger.getLogger(CLSS)
        private const val EXIT_WAIT_INTERVAL: Long = 1000
        private val LOG_ROOT = CLSS.lowercase(Locale.getDefault())

        /**
         * Entry point for the application that contains the robot Java
         * code for control of the appendages, among other things.
         *
         * Usage: bert <config>
         *
         * @param args command-line arguments
        </config> */
        @JvmStatic
        fun main(args: Array<String>) {

            // Make sure there is command-line argument
            if (args.size < 1) {
                println(USAGE)
                System.exit(1)
            }

            // Analyze command-line argument to obtain the configuration file path.
            val arg = args[0]
            val path = Paths.get(arg)
            PathConstants.setHome(path)
            // Setup logging to use only a file appender to our logging directory
            LoggerUtility.getInstance().configureRootLogger(LOG_ROOT)
            val model = RobotCommandModel(PathConstants.CONFIG_PATH)
            model.populate()
            Database.getInstance().startup(PathConstants.DB_PATH)
            val runner = Command(model)
            runner.createControllers()
            Runtime.getRuntime().addShutdownHook(ShutdownHook(runner))
            runner.startup()
            runner.start()
        }
    }
}