/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.control.solver

import chuckcoughlin.bert.common.PathConstants
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.util.LoggerUtility
import chuckcoughlin.bert.control.model.TestRobotModel
import java.nio.file.Paths
import java.util.*
import java.util.logging.Logger

/**
 * This class solves a fixed configuration.
 */
class TestSolver {



    private val CLSS = "Solver"
    private val LOGGER = Logger.getLogger(CLSS)
    private val ERROR_POSITION = doubleArrayOf(0.0, 0.0, 0.0)

    /**
     * Test forward kinematic calculations of various positions of the robot skeleton.
     * We rely on configuration file in $BERT_HOME/etc on the development machine.
     */
    fun main(args: Array<String>) {
        // Analyze command-line argument to obtain the robot root directory.
        val arg = args[0]
        val path = Paths.get(arg)
        PathConstants.setHome(path)
        // Setup logging to use only a file appender to our logging directory
        val LOG_ROOT = CLSS.lowercase(Locale.getDefault())
        LoggerUtility.configureTestLogger(LOG_ROOT)
        // Analyze the xml for motor configurations. Initialize the motor configurations.
        val model = TestRobotModel(PathConstants.CONFIG_PATH!!)
        model.populate() //
        val solver = Solver()
        solver.configure(model.motors, PathConstants.URDF_PATH!!)

        //solver.setJointPosition(Joint.ABS_Y,90.);
        val xyz = solver.getPosition(Joint.ABS_Y) // Just to top of pelvis
        println(String.format("%s (0.2,0,.114): xyz = %.2f,%.2f,%.2f ",
            Joint.ABS_Y.name,xyz!![0],xyz[1],xyz[2]))
    }

}