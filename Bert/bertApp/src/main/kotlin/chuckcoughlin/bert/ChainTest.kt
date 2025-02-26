package chuckcoughlin.bert


import chuckcoughlin.bert.common.PathConstants
import chuckcoughlin.bert.common.model.Chain
import chuckcoughlin.bert.common.model.Chain.root
import chuckcoughlin.bert.common.model.Extremity
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.model.Solver
import chuckcoughlin.bert.common.model.URDFModel
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
object ChainTest {
    val root = Chain.root

    /**
     * Test construction of the chain of robot "limbs" based on the URDF file in
     * $BERT_HOME/etc on the development machine.
     */
    fun execute() {
        setMotorPositions()

        // Test the links to some extremities
        println(String.format("==================== %s ===========================================",CLSS ))
        println(String.format("%s: root = %s ", CLSS, root.bone.name))

        println("======== Test LEFT_EAR to PELVIS subchain")
        var subchain = Chain.partialChainToExtremity(Extremity.LEFT_EAR)
        for (link in subchain) {
            println(String.format("\t%s ", link.bone.name))
        }
        println("======== Test RIGHT_FINGER to PELVIS subchain")
        subchain = Chain.partialChainToExtremity(Extremity.RIGHT_FINGER)
        for (link in subchain) {
            println(String.format("\t%s ", link.bone.name))
        }
        println("======== Test ABS_X to PELVIS subchain")
        subchain = Chain.partialChainToJoint(Joint.ABS_X)
        for (link in subchain) {
            println(String.format("\t%s ", link.bone.name))
        }
    }

    private val CLSS = "ChainTest"

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
}

