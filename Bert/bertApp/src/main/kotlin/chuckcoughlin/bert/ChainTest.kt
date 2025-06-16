package chuckcoughlin.bert


import chuckcoughlin.bert.common.model.*

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
        println("======== Test IMU orientation 0,0,0 ")
        IMU.quaternion.setRoll(0.0)
        IMU.quaternion.setPitch(0.0)
        IMU.quaternion.setYaw(0.0)
        IMU.update()
        println(String.format("\tABS- (0,0,0) = %s ",Solver.computeLocation(Joint.ABS_Y).toText()))
        IMU.quaternion.setRoll(1.57)    // 90 deg
        IMU.quaternion.setPitch(0.0)
        IMU.quaternion.setYaw(0.0)
        IMU.update()
        println(String.format("\tABS-Y (90,0.0) = %s ",Solver.computeLocation(Joint.ABS_Y).toText()))
        IMU.quaternion.setRoll(0.0)
        IMU.quaternion.setPitch(1.57)
        IMU.quaternion.setYaw(0.0)
        IMU.update()
        println(String.format("\tABS-X (0,90,0) = %s ",Solver.computeLocation(Joint.ABS_X).toText()))
        IMU.quaternion.setRoll(0.0)    // reset IMU
        IMU.quaternion.setPitch(0.0)
        IMU.quaternion.setYaw(0.0)
        IMU.update()
        println("======== Test Joints along back - home pose")
        println(String.format("\tABS-Y = %s ",Solver.computeLocation(Joint.ABS_Y).toText()))
        println(String.format("\tABS-X = %s ",Solver.computeLocation(Joint.ABS_X).toText()))
        println(String.format("\tABS-Z = %s      (0.0,0.0,121.6)",Solver.computeLocation(Joint.ABS_Z).toText()))
        println(String.format("\tCHEST_Y = %s     (2.8,0.0,201.5) ",Solver.computeLocation(Joint.CHEST_Y).toText()))
        println(String.format("\tCHEST_X = %s     (2.8,0.0,193.5) ",Solver.computeLocation(Joint.CHEST_X).toText()))
        println("######## VERIFIED TO HERE ####################")
        println(String.format("\tNECK_Z = %s ",Solver.computeLocation(Joint.NECK_Z).toText()))
        println(String.format("\tNECK_Y = %s ",Solver.computeLocation(Joint.NECK_Y).toText()))
        println(String.format("\tNOSE = %s ",Solver.computeLocation(Appendage.NOSE).toText()))
        println(String.format("\tLEFT_EAR = %s ",Solver.computeLocation(Appendage.LEFT_EAR).toText()))
        println(String.format("\tRIGHT_EAR = %s ",Solver.computeLocation(Appendage.RIGHT_EAR).toText()))
        println(String.format("\tLEFT_EYE = %s ",Solver.computeLocation(Appendage.LEFT_EYE).toText()))
        println(String.format("\tRIGHT_EYE = %s ",Solver.computeLocation(Appendage.RIGHT_EYE).toText()))
        println("======== Test Joints along upper body sides - home pose")
        println(String.format("\tRIGHT_SHOULDER_Y = %s ",Solver.computeLocation(Joint.RIGHT_SHOULDER_Y).toText()))
        println(String.format("\tLEFT_SHOULDER_Y = %s ",Solver.computeLocation(Joint.LEFT_SHOULDER_Y).toText()))
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

