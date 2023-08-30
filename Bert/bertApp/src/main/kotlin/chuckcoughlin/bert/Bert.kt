/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert

import chuckcoughlin.bert.common.PathConstants
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.util.LoggerUtility
import chuckcoughlin.bert.common.util.ShutdownHook
import chuckcoughlin.bert.control.solver.Solver
import chuckcoughlin.bert.dispatch.Dispatcher
import chuckcoughlin.bert.sql.db.Database
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import java.util.*
import java.util.logging.Logger

class Bert() {
    private val CLSS = "Bert"
    val USAGE = "Usage: bert [-o] <robot_root>"
    val LOGGER = Logger.getLogger(CLSS)
    val LOG_ROOT = CLSS.lowercase(Locale.getDefault())
}
/**
 * Entry point for the main application that receives commands, processes
 * them through the serial interfaces to the motors and returns results.
 *
 * Usage: Usage: bertApp [-o] <robot_root>
 *
 * @param args command-line arguments
 */
fun main(args: Array<String>) {
    val app = Bert()
    // Make sure there is command-line argument
    if (args.isEmpty()) {
        println(app.USAGE)
        System.exit(1)
    }

    var arg = ConfigurationConstants.NO_VALUE
    var offline = true
    var error = true
    // Analyze command-line argument to obtain the robot root directory.
    // -o implies "offline", no serial or bluetooth communication
    for( a:String in args) {
        if(a.startsWith("-")) {   // Option
            if( a.equals("-o")) offline = true
            else {
               break
            }
        }
        else {
            arg = a
            error = false
            break
        }
    }
    if( error) {
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
    RobotModel.populate() // Analyze the xml for controllers and motors
    RobotModel.online = !offline
    //
    Database.startup(PathConstants.DB_PATH)
    val solver = Solver()
    solver.configure(RobotModel.motors, PathConstants.URDF_PATH)
    val dispatcher = Dispatcher(solver)
    Runtime.getRuntime().addShutdownHook(Thread(ShutdownHook(dispatcher)))
    runBlocking {
        dispatcher.start()
    }
}

