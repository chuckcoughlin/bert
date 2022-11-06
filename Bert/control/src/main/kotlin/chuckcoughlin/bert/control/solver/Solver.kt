/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.control.solver

import chuckcoughlin.bert.common.PathConstants
import chuckcoughlin.bert.common.model.Appendage
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.MotorConfiguration
import chuckcoughlin.bert.common.util.LoggerUtility
import chuckcoughlin.bert.control.model.Link
import chuckcoughlin.bert.control.model.URDFModel
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.logging.Logger

/**
 * This class handles various computations pertaining to the robot,
 * including: trajectory planning. Note that the same link object
 * may belong to several chains.
 */
class Solver {
    private val model: URDFModel
    private var motorConfigurations: Map<Joint?, MotorConfiguration>?

    /**
     * Constructor:
     */
    init {
        model = URDFModel()
        motorConfigurations = null
    }

    /**
     * @return the tree of links which describes the robot.
     */
    fun getModel(): URDFModel {
        return model
    }

    /**
     * Traverse the tree, setting the current angles from the
     * motor configurations. Mark the links as "dirty".
     */
    fun setTreeState() {
        val links: Collection<Link> = model.chain.getLinks()
        for (link in links) {
            link.setDirty()
            val joint: Joint? = link.linkPoint.joint
            val mc: MotorConfiguration? = motorConfigurations!![joint]
            link.jointAngle = mc.position
        }
    }

    /**
     * Analyze the URDF file for robot geometry. This must be called before
     * we set a tree state.
     * @param mc a map of MotorConfigurations
     * @param urdfPath
     */
    fun configure(mc: Map<Joint?, MotorConfiguration>?, urdfPath: Path) {
        motorConfigurations = mc
        LOGGER.info(String.format("%s.configure: URDF file(%s)", CLSS, urdfPath.toAbsolutePath().toString()))
        model.analyzePath(urdfPath)
    }

    /**
     * Return the position of a specified appendage in x,y,z coordinates in meters from the
     * robot origin in the pelvis.
     */
    fun getPosition(appendage: Appendage?): DoubleArray? {
        val subchain: List<Link> = model.chain.partialChainToAppendage(appendage)
        return if (subchain.size > 0) subchain[0].coordinates else ERROR_POSITION
    }

    /**
     * Return the position of a specified joint in x,y,z coordinates in meters from the
     * robot origin in the pelvis in the inertial reference frame.
     */
    fun getPosition(joint: Joint?): DoubleArray? {
        val subchain: List<Link> = model.chain.partialChainToJoint(joint)
        return if (subchain.size > 0) subchain[0].coordinates else ERROR_POSITION
    }

    fun getMotorConfigurations(): Map<Joint?, MotorConfiguration>? {
        return motorConfigurations
    }

    /**
     * Set the position of a joint. This is primarily for testing. It does not
     * cause a serial write to the motor.
     * @param joint
     * @param pos
     */
    fun setJointPosition(joint: Joint?, pos: Double) {
        val mc: MotorConfiguration? = motorConfigurations!![joint]
        mc.position = pos
    }

    companion object {
        private const val CLSS = "Solver"
        private val LOGGER = Logger.getLogger(CLSS)
        private val ERROR_POSITION = doubleArrayOf(0.0, 0.0, 0.0)

        /**
         * Test forward kinematic calculations of various positions of the robot skeleton.
         * We rely on configuration file in $BERT_HOME/etc on the development machine.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            // Analyze command-line argument to obtain the robot root directory.
            val arg = args[0]
            val path = Paths.get(arg)
            PathConstants.setHome(path)
            // Setup logging to use only a file appender to our logging directory
            val LOG_ROOT = CLSS.lowercase(Locale.getDefault())
            LoggerUtility.getInstance().configureTestLogger(LOG_ROOT)
            // Analyze the xml for motor configurations. Initialize the motor configurations.
            val model = TestRobotModel(PathConstants.CONFIG_PATH)
            model.populate() //
            val solver = Solver()
            solver.configure(model.getMotors(), PathConstants.URDF_PATH)

            //solver.setJointPosition(Joint.ABS_Y,90.);
            val xyz: DoubleArray = solver.getPosition(Joint.ABS_Y) // Just to top of pelvis
            println(
                java.lang.String.format(
                    "%s (0.2,0,.114): xyz = %.2f,%.2f,%.2f ",
                    Joint.ABS_Y.name(),
                    xyz[0],
                    xyz[1],
                    xyz[2]
                )
            )
        }
    }
}