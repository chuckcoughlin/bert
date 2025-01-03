package chuckcoughlin.bert


import chuckcoughlin.bert.common.PathConstants
import chuckcoughlin.bert.common.model.Chain
import chuckcoughlin.bert.common.model.Extremity
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.model.Solver
import chuckcoughlin.bert.common.util.LoggerUtility
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

    Solver.configure(RobotModel.motorsByJoint, PathConstants.URDF_PATH)
    val chain: Chain = Solver.model.chain
    val root = chain.root
    println(String.format("%s: root = %s ", CLSS, root.bone.name))
    // Test the links to some extremities
    println("=========================================================================")
    var subchain = chain.partialChainToExtremity(Extremity.LEFT_EAR)
    for (link in subchain) {
        println(String.format("\t%s ", link.bone.name))
    }
    println("=========================================================================")
    subchain = chain.partialChainToExtremity(Extremity.RIGHT_FINGER)
    for (link in subchain) {
        println(String.format("\t%s ", link.bone.name))
    }
    println("=========================================================================")
    subchain = chain.partialChainToExtremity(Extremity.RIGHT_TOE)
    for (link in subchain) {
        println(String.format("\t%s ", link.bone.name))
    }
    println("=========================================================================")
    subchain = chain.partialChainToJoint(Joint.ABS_Y)
    for (link in subchain) {
        println(String.format("\t%s ", link.bone.name))
    }
}

private val CLSS = "TestChain"

/**
 * Set the initial positions of the motors to "home"!
 */
fun setMotorPositions() {
    for (joint in RobotModel.motorsByJoint.keys) {
        val mc = RobotModel.motorsByJoint.get(joint)
        // Set some reasonable values from the "home" pose.
        when (joint) {
            Joint.ABS_X -> mc!!.angle = 180.0
            Joint.ABS_Y -> mc!!.angle = 180.0
            Joint.ABS_Z -> mc!!.angle = 0.0
            Joint.BUST_X -> mc!!.angle = 180.0
            Joint.BUST_Y -> mc!!.angle = 180.0
            Joint.NECK_Y -> mc!!.angle = 0.0
            Joint.NECK_Z -> mc!!.angle = 0.0
            Joint.LEFT_ANKLE_Y -> mc!!.angle = 90.0
            Joint.LEFT_SHOULDER_Z -> mc!!.angle = 0.0
            Joint.LEFT_ELBOW_Y -> mc!!.angle = 180.0
            Joint.LEFT_HIP_X -> mc!!.angle = 180.0
            Joint.LEFT_HIP_Y -> mc!!.angle = 180.0
            Joint.LEFT_HIP_Z -> mc!!.angle = 0.0
            Joint.LEFT_KNEE_Y -> mc!!.angle = 180.0
            Joint.LEFT_SHOULDER_X -> mc!!.angle = 180.0
            Joint.LEFT_SHOULDER_Y -> mc!!.angle = 180.0
            Joint.RIGHT_ANKLE_Y -> mc!!.angle = 90.0
            Joint.RIGHT_SHOULDER_Z -> mc!!.angle = 0.0
            Joint.RIGHT_ELBOW_Y -> mc!!.angle = 180.0
            Joint.RIGHT_HIP_X -> mc!!.angle = 180.0
            Joint.RIGHT_HIP_Y -> mc!!.angle = 180.0
            Joint.RIGHT_HIP_Z -> mc!!.angle = 0.0
            Joint.RIGHT_KNEE_Y -> mc!!.angle = 180.0
            Joint.RIGHT_SHOULDER_X -> mc!!.angle = 180.0
            Joint.RIGHT_SHOULDER_Y -> mc!!.angle = 180.0
            Joint.NONE -> mc!!.angle = 0.0
        }
    }
}
