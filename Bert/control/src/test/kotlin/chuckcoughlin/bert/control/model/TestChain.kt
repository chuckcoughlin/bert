package chuckcoughlin.bert.control.model


import chuckcoughlin.bert.common.PathConstants
import chuckcoughlin.bert.common.model.Appendage
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.util.LoggerUtility
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
        val model = TestRobotModel(PathConstants.CONFIG_PATH!!)
        model.populate() //
        val solver = Solver()
        solver.configure(model.motors, PathConstants.URDF_PATH!!)
        val chain: Chain = solver.getModel().chain
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

    init {

    }
}
