package chuckcoughlin.bert


import chuckcoughlin.bert.common.PathConstants
import chuckcoughlin.bert.common.model.Appendage
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.util.LoggerUtility
import chuckcoughlin.bert.control.model.Chain
import chuckcoughlin.bert.control.solver.Solver
import java.nio.file.Paths
import java.util.*

/**
 * A Chain represents a tree of Links starting with the
 * "root" link. The position of links within the chain are
 * all relative to the root link (i.e. origin). The URDF
 * file format doesn't define things in the most convenient
 * order.
 *
 * Changes to the "inertial frame" as detected by the IMU
 * are all handled here.
 */
class TestChain {
}

/**
 * Test construction of the chain of robot "limbs" based on the URDF file in
 * $BERT_HOME/etc on the development machine.
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
    RobotModel.startup(PathConstants.CONFIG_PATH)
    RobotModel.populate() //
    setMotorPositions()
    val solver = Solver()
    solver.configure(RobotModel.motors, PathConstants.URDF_PATH)
    val chain: Chain = solver.model.chain
    val root = chain.root
    println(String.format("%s: root = %s ", CLSS, root!!.name))
    // Test the links to some appendages
    println("=========================================================================")
    var subchain = chain.partialChainToAppendage(Appendage.LEFT_EAR)
    for (link in subchain) {
        println(String.format("\t%s ", link.name))
    }
    println("=========================================================================")
    subchain = chain.partialChainToAppendage(Appendage.RIGHT_FINGER)
    for (link in subchain) {
        println(String.format("\t%s ", link.name))
    }
    println("=========================================================================")
    subchain = chain.partialChainToAppendage(Appendage.RIGHT_TOE)
    for (link in subchain) {
        println(String.format("\t%s ", link.name))
    }
    println("=========================================================================")
    subchain = chain.partialChainToJoint(Joint.ABS_Y)
    for (link in subchain) {
        println(String.format("\t%s ", link.name))
    }
}

private val CLSS = "TestChain"

/**
 * Set the initial positions of the motors to "home"!
 */
fun setMotorPositions() {
    for (joint in RobotModel.motors.keys) {
        val mc = RobotModel.motors.get(joint)
        // Set some reasonable values from the "home" pose.
        when (joint) {
            Joint.ABS_X -> mc!!.position = 180.0
            Joint.ABS_Y -> mc!!.position = 180.0
            Joint.ABS_Z -> mc!!.position = 0.0
            Joint.BUST_X -> mc!!.position = 180.0
            Joint.BUST_Y -> mc!!.position = 180.0
            Joint.NECK_Y -> mc!!.position = 0.0
            Joint.NECK_Z -> mc!!.position = 0.0
            Joint.LEFT_ANKLE_Y -> mc!!.position = 90.0
            Joint.LEFT_ARM_Z -> mc!!.position = 0.0
            Joint.LEFT_ELBOW_Y -> mc!!.position = 180.0
            Joint.LEFT_HIP_X -> mc!!.position = 180.0
            Joint.LEFT_HIP_Y -> mc!!.position = 180.0
            Joint.LEFT_HIP_Z -> mc!!.position = 0.0
            Joint.LEFT_KNEE_Y -> mc!!.position = 180.0
            Joint.LEFT_SHOULDER_X -> mc!!.position = 180.0
            Joint.LEFT_SHOULDER_Y -> mc!!.position = 180.0
            Joint.RIGHT_ANKLE_Y -> mc!!.position = 90.0
            Joint.RIGHT_ARM_Z -> mc!!.position = 0.0
            Joint.RIGHT_ELBOW_Y -> mc!!.position = 180.0
            Joint.RIGHT_HIP_X -> mc!!.position = 180.0
            Joint.RIGHT_HIP_Y -> mc!!.position = 180.0
            Joint.RIGHT_HIP_Z -> mc!!.position = 0.0
            Joint.RIGHT_KNEE_Y -> mc!!.position = 180.0
            Joint.RIGHT_SHOULDER_X -> mc!!.position = 180.0
            Joint.RIGHT_SHOULDER_Y -> mc!!.position = 180.0
            Joint.NONE -> mc!!.position = 0.0
        }
    }
}
