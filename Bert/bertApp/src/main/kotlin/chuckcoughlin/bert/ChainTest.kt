package chuckcoughlin.bert


import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.common.solver.ForwardSolver

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

    /**
     * Test construction of the chain of robot "limbs" based on the URDF file in
     * $BERT_HOME/etc on the development machine.
     */
    fun execute() {
        setMotorPositions()

        // Test the links to some extremities
        println(String.format("==================== %s ===========================================",CLSS ))

        println("======== Test LEFT_EAR to PELVIS sub-chain")
        var subchain = Chain.partialChainToAppendage(Appendage.LEFT_EAR)
        for (link in subchain) {
            println(String.format("\t%s ", link.name))
        }
        println("======== Test RIGHT_FINGER to PELVIS sub-chain")
        subchain = Chain.partialChainToAppendage(Appendage.RIGHT_FINGER)
        for (link in subchain) {
            println(String.format("\t%s ", link.name))
        }
        println("======== Test ABS_X to PELVIS sub-chain")
        subchain = Chain.partialChainToJoint(Joint.ABS_X)
        for (link in subchain) {
            println(String.format("\t%s ", link.name))
        }
        println("======== Test ABS_Y for IMU orientations (should match)")
        IMU.quaternion.setRoll(0.0)
        IMU.quaternion.setPitch(0.0)
        IMU.quaternion.setYaw(0.0)
        IMU.update()
        println(String.format("\tABS-Y   (IMU=0,0,0) = %s ", ForwardSolver.computePositionDescription(Joint.ABS_Y)))
        IMU.quaternion.setRoll(1.57)    // 90 deg
        IMU.quaternion.setPitch(0.0)
        IMU.quaternion.setYaw(0.0)
        IMU.update()
        println(String.format("\tABS-Y (IMU=90,0.0) = %s ", ForwardSolver.computePositionDescription(Joint.ABS_Y)))
        IMU.quaternion.setRoll(0.0)
        IMU.quaternion.setPitch(1.57)
        IMU.quaternion.setYaw(0.0)
        IMU.update()
        println(String.format("\tABS-Y (IMU=0,90,0) = %s ", ForwardSolver.computePositionDescription(Joint.ABS_Y)))
        IMU.quaternion.setRoll(0.0)
        IMU.quaternion.setPitch(0.0)
        IMU.quaternion.setYaw(1.57)
        IMU.update()
        println(String.format("\tABS-Y (IMU=0,0,90) = %s ", ForwardSolver.computePositionDescription(Joint.ABS_Y)))
        IMU.quaternion.setRoll(0.0)    // reset IMU
        IMU.quaternion.setPitch(0.0)
        IMU.quaternion.setYaw(0.0)
        IMU.update()
        println(String.format("\tABS-Y   (IMU=0,0,0) = %s ", ForwardSolver.computePositionDescription(Joint.ABS_Y)))
        println("======== Test Joints along back - home pose")
        println(String.format("\tABS-Y = %s ", ForwardSolver.computePositionDescription(Joint.ABS_Y)))
        println(String.format("\tABS-X = %s ", ForwardSolver.computePositionDescription(Joint.ABS_X)))
        println(String.format("\tABS-Z = %s       (0.0,0.0,121.6)", ForwardSolver.computePositionDescription(Joint.ABS_Z)))
        println(String.format("\tCHEST_Y = %s     (2.8,0.0,201.5) ", ForwardSolver.computePositionDescription(Joint.CHEST_Y)))
        println(String.format("\tCHEST_X = %s     (2.8,0.0,193.5) ", ForwardSolver.computePositionDescription(Joint.CHEST_X)))
        println(String.format("\tNECK_Z = %s      (7.8,0.0,277.5)", ForwardSolver.computePositionDescription(Joint.NECK_Z)))
        println(String.format("\tNECK_Y = %s ", ForwardSolver.computePositionDescription(Joint.NECK_Y)))
        println(String.format("\tNOSE   = %s ", ForwardSolver.computePositionDescription(Appendage.NOSE)))
        println(String.format("\tLEFT_EAR  = %s ", ForwardSolver.computePositionDescription(Appendage.LEFT_EAR)))
        println(String.format("\tRIGHT_EAR = %s ", ForwardSolver.computePositionDescription(Appendage.RIGHT_EAR)))
        println(String.format("\tLEFT_EYE  = %s ", ForwardSolver.computePositionDescription(Appendage.LEFT_EYE)))
        println(String.format("\tRIGHT_EYE = %s ", ForwardSolver.computePositionDescription(Appendage.RIGHT_EYE)))
        println("         BACK JOINTS/APPENDAGES VERIFIED ####################")
        println("======== Test Joints along upper body sides - home pose")
        println(String.format("\tRIGHT_SHOULDER_Y = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_SHOULDER_Y)))
        println(String.format("\tLEFT_SHOULDER_Y  = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_SHOULDER_Y)))
        println(String.format("\tRIGHT_SHOULDER_X = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_SHOULDER_X)))
        println(String.format("\tLEFT_SHOULDER_X  = %s (6.8, 105.5, 243.5)", ForwardSolver.computePositionDescription(Joint.LEFT_SHOULDER_X)))
        println(String.format("\tRIGHT_SHOULDER_Z = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_SHOULDER_Z)))
        println(String.format("\tLEFT_SHOULDER_Z  = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_SHOULDER_Z)))
        println(String.format("\tRIGHT_ELBOW_Y    = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_ELBOW_Y)))
        println(String.format("\tLEFT_ELBOW_Y     = %s (-3.2, 141.8, 143.8)", ForwardSolver.computePositionDescription(Joint.LEFT_ELBOW_Y)))
        println(String.format("\tRIGHT_FINGER = %s ", ForwardSolver.computePositionDescription(Appendage.RIGHT_FINGER)))
        println(String.format("\tLEFT_FINGER  = %s ", ForwardSolver.computePositionDescription(Appendage.LEFT_FINGER)))
        println("         UPPER JOINTS/APPENDAGES VERIFIED ####################")
        println("======== Test Joints along lower body sides - home pose")
        println(String.format("\tRIGHT_HIP_X = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_HIP_X)))
        println(String.format("\tLEFT_HIP_X  = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_HIP_X)))
        println(String.format("\tRIGHT_HIP_Z = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_HIP_Z)))
        println(String.format("\tLEFT_HIP_Z  = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_HIP_Z)))
        println(String.format("\tRIGHT_HIP_Y = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_HIP_Y)))
        println(String.format("\tLEFT_HIP_Y  = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_HIP_Y)))
        println(String.format("\tRIGHT_KNEE_Y = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_KNEE_Y)))
        println(String.format("\tLEFT_KNEE_Y  = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_KNEE_Y)))
        println(String.format("\tRIGHT_ANKLE_Y= %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_ANKLE_Y)))
        println(String.format("\tLEFT_ANKLE_Y = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_ANKLE_Y)))
        println(String.format("\tRIGHT_HEEL = %s ", ForwardSolver.computePositionDescription(Appendage.RIGHT_HEEL)))
        println(String.format("\tLEFT_HEEL  = %s ", ForwardSolver.computePositionDescription(Appendage.LEFT_HEEL)))
        println(String.format("\tRIGHT_TOE  = %s ", ForwardSolver.computePositionDescription(Appendage.RIGHT_TOE)))
        println(String.format("\tLEFT_TOE  = %s ", ForwardSolver.computePositionDescription(Appendage.LEFT_TOE)))
        println("         LOWER JOINTS/APPENDAGES VERIFIED ####################")
    }

    const val CLSS = "ChainTest"

    /**
     * Set the initial positions of the motors to "home"!
     */
    private fun setMotorPositions() {
        for (joint in RobotModel.motorsByJoint.keys) {
            val mc = RobotModel.motorsByJoint.get(joint)
            // Set some reasonable values from the "home" pose.
            when (joint) {
                Joint.ABS_X -> mc!!.angle = 180.0
                Joint.ABS_Y -> mc!!.angle = 180.0
                Joint.ABS_Z -> mc!!.angle = 0.0
                Joint.CHEST_X -> mc!!.angle = 180.0
                Joint.CHEST_Y -> mc!!.angle = 180.0
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
                Joint.IMU -> {}
            }
        }
    }
}

