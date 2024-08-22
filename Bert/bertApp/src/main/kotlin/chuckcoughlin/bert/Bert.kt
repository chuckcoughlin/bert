/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert

import chuckcoughlin.bert.common.PathConstants
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.util.LoggerUtility
import chuckcoughlin.bert.common.util.ShutdownHook
import chuckcoughlin.bert.common.model.Solver
import chuckcoughlin.bert.dispatch.Dispatcher

import chuckcoughlin.bert.sql.db.Database
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import java.util.*
import java.util.logging.Logger

class Bert() {
    val CLSS = "Bert"
    val USAGE = "Usage: bert [-bst] <robot_root>"
    val LOGGER = Logger.getLogger(CLSS)
    val LOG_ROOT = CLSS.lowercase(Locale.getDefault())
}
/**
 * Entry point for the main application that receives commands, processes
 * them through the serial interfaces to the motors and returns results.`
 *
 * Usage: Usage: bertApp [-o] <robot_root>
 *
 * @param args command-line arguments
 */
@DelicateCoroutinesApi
fun main(args: Array<String>) {
    val app = Bert()
    // Make sure there is command-line argument
    if (args.isEmpty()) {
        println(app.USAGE)
        System.exit(1)
    }

    var arg = ConfigurationConstants.NO_VALUE
    var error = true
    // Analyze command-line argument to obtain the robot root directory.
    // -n use network (wifi) communication
    // -s use serial communications to servos
    // -t use a local terminal connection to input commands
    var network = false
    var serial    = false
    var terminal  = false
    // debug
    var dbg = ""
    for (a: String in args) {
        if (a.startsWith("-d")) {     // Debugging flags
            dbg = a.substring(2)
        }
        else if (a.startsWith("-")) {   // Option
            if (a.contains("n")) network = true
            if (a.contains("s")) serial    = true
            if (a.contains("t")) terminal  = true
        }
        else {
            arg = a
            error = false
            break
        }
    }
    if (error) {
        println(app.USAGE)
        System.exit(1)
    }

    val path = Paths.get(arg)
    PathConstants.setHome(path)
    // Setup logging to use only a file appender to our logging directory
    LoggerUtility.configureRootLogger(app.LOG_ROOT)

    // The RobotModel is a singleton that describes
    // any configurable robot parameters.
    RobotModel.startup(PathConstants.CONFIG_PATH)
    // Enable internal logging
    RobotModel.debug = dbg
    RobotModel.populate() // Analyze the xml for controllers and motors
    // We reserve several options for the command line as opposed
    // to the configuration file. Set model values here.
    RobotModel.useNetwork = network
    RobotModel.useSerial    = serial
    RobotModel.useTerminal  = terminal

    Database.startup(PathConstants.DB_PATH)
    Solver.configure(RobotModel.motorsByJoint, PathConstants.URDF_PATH)
    val dispatcher = Dispatcher()
    Runtime.getRuntime().addShutdownHook(Thread(ShutdownHook(dispatcher)))
    runBlocking {
        dispatcher.execute()
    }
}

