/**
 * Copyright 2022-2023 Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.command.controller

import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.PathConstants
import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.controller.SocketController
import chuckcoughlin.bert.common.message.CommandType
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.util.LoggerUtility
import chuckcoughlin.bert.common.util.ShutdownHook
import chuckcoughlin.bert.speech.process.MessageTranslator
import chuckcoughlin.bert.sql.db.Database
import kotlinx.coroutines.channels.Channel
import java.lang.Thread.sleep
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger

/**
 * This is the class that handles spoken commands and forwards
 * them on to the central launcher. It also handles database actions
 * involving playback and record.
 */
class Command(parent: Controller,req : Channel<MessageBottle>,rsp: Channel<MessageBottle>) : Controller {
    private var tabletController: BluetoothController? = null
    private val messageTranslator: MessageTranslator
    private var dispatchController: SocketController? = null
    private val busy: Condition
    private var currentRequest: MessageBottle? = null
    private val lock: Lock
    private var ignoring: Boolean

    /**
     * This class routes requests/responses between the Dispatcher and "blueserverd" daemon.
     * Communication with the Dispatcher is via Kotlin channels and communication with
     * Bluetooth is via a socket..
     */
    fun createControllers() {
        tabletController = BluetoothController(this, RobotModel.blueserverPort)
        val hostName: String = RobotModel.getProperty(ConfigurationConstants.PROPERTY_HOSTNAME, "localhost")
        val sockets: Map<String, Int> = RobotModel.sockets
        val walker = sockets.keys.iterator()
        val key = walker.next()
        val port = sockets[key]!!
        dispatchController = SocketController(this, ControllerType.COMMAND.name, hostName, port)
    }

    /**
     * Loop forever reading from the bluetooth daemon (representing the tablet) and forwarding the resulting requests
     * via socket to the server (launcher). We accept its responses and forward back to the tablet.
     * Communication with the tablet consists of simple strings, plus a 4-character header.
     */
     fun run() {
        try {
            while (true) {
                lock.lock()
                try {
                    busy.await()
                    if (currentRequest == null) break
                    if (currentRequest.type.equals(RequestType.COMMAND) &&
                        BottleConstants.COMMAND_HALT.equalsIgnoreCase(
                            currentRequest.getProperties().get(BottleConstants.COMMAND_NAME)
                        )
                    ) {
                        dispatchController.receiveRequest(currentRequest) // halt the dispatcher as well
                        sleep(EXIT_WAIT_INTERVAL)
                        break
                    }
                    else if (isLocalRequest(currentRequest)) {
                        // Handle local request -create response
                        val response: MessageBottle = handleLocalRequest(currentRequest)
                        if (response != null) handleResponse(response)
                    }
                    else if (!ignoring) {
                        dispatchController.receiveRequest(currentRequest)
                    }
                }
                catch (ie: InterruptedException) {}
                finally {
                    lock.unlock()
                }
            }
        }
        catch (ex: Exception) {
            ex.printStackTrace()
        }
        finally {
            shutdown()
        }
        Database.shutdown()
        System.exit(0)
    }

    override suspend fun start() {
        dispatchController.start()
        tabletController!!.start()
    }

    override suspend fun stop() {
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
        if (request.type.equals(RequestType.COMMAND)) {
            val command: CommandType = request.command
            LOGGER.warning(String.format("%s.handleLocalRequest: command=%s", CLSS, command))
            if( command.equals(CommandType.SLEEP) ) {
                ignoring = true
            }
            else if( command.equals(CommandType.WAKE) ) {
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
            if (cmd == CommandType.SLEEP || cmd == CommandType.WAKE) {
                return true
            }
        }
        return false
    }

    private val CLSS = "Command"
    private val LOGGER = Logger.getLogger(CLSS)
    private val EXIT_WAIT_INTERVAL: Long = 1000
    override var controllerName = CLSS

    init {
        controllerName = RobotModel.getControllerForType(ControllerType.COMMAND)
        lock = ReentrantLock()
        busy = lock.newCondition()
        ignoring = false
        messageTranslator = MessageTranslator()
    }
}