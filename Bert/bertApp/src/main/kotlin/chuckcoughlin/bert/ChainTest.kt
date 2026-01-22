package chuckcoughlin.bert


import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.common.solver.ForwardSolver

/**
 * Test construction of the chain of robot "limbs" based on the URDF file in
 * $BERT_HOME/etc.
 *
 * Changes to the "inertial frame" are expressed by the IMU
 * rotation and are tested also.
 */
object ChainTest {

    fun execute() {
        // setMotorPositions()
        val tree = ForwardSolver.tree
        var q : Quaternion

        // Test the links to some extremities
        println(String.format("==================== %s ===========================================",CLSS ))

        println("======== Test LEFT_EAR to PELVIS position-chain")
        var chain = tree.createLinkChain(Joint.LEFT_EAR)
        for (link in chain) {
            println(String.format("\t%s ", link.end.name))
        }
        println("======== Test RIGHT_FINGER to PELVIS link-chain")
        chain = tree.createLinkChain(Joint.RIGHT_FINGER)
        for (link in chain) {
            println(String.format("\t%s ", link.end.name))
        }
        println("======== Test ABS_X to PELVIS link-chain")
        chain = tree.createLinkChain(Joint.ABS_X)
        for (link in chain) {
            println(String.format("\t%s ", link.end.name))
        }
        println("======== Test ABS_Y for IMU orientations (directions should match)")
        val link1 = tree.getJointLink(Joint.ABS_Y)  // Link from IMU to ABS_Y
        val root = tree.getOrCreateJointPosition(link1.sourceJoint)  // IMU
        val absy = tree.getOrCreateJointPosition(link1.endJoint)     // ABS_Y
        link1.setJointAngle(link1.home)
        link1.setRpy(0.0,0.0,0.0)
        q = link1.quaternionForSource(root)
        q = Quaternion.computeEnd(link1,q)
        absy.updateFromQuaternion(q)
        println(String.format("\t(IMU=12,0,62 [0,0,0]) = %s [%s]", Joint.IMU.quaternion.positionToText(),q.directionToText()))
        link1.setRpy(90.0,0.0,0.0)
        println(String.format("\t(IMU=12,0,62 [90,0,0]) = %s [%s]", Joint.IMU.quaternion.positionToText(),q.directionToText()))
/**
        IMU.setRoll(0.0)
        IMU.setPitch(90.0)
        IMU.setYaw(0.0)
        println(String.format("\tABS-Y (IMU=0,90,0) = %s ", ForwardSolver.computePositionDescription(Joint.ABS_Y.name)))
        IMU.setRoll(0.0)
        IMU.setPitch(0.0)
        IMU.setYaw(90.0)
        println(String.format("\tABS-Y (IMU=0,0,90) = %s ", ForwardSolver.computePositionDescription(Joint.ABS_Y.name)))
        IMU.setRoll(0.0)    // reset IMU
        IMU.setPitch(0.0)
        IMU.setYaw(0.0)
        println(String.format("\tABS-Y  (IMU=0,0,0) = %s ", ForwardSolver.computePositionDescription(Joint.ABS_Y.name)))
        println("======== Test Joints along back to head - home position")
        RobotModel.setTreeToHome(tree)
        println(String.format("\tABS-Y = %s (12.0,0.0,62) [0,0,0]", ForwardSolver.computePositionDescription(Joint.ABS_Y.name)))
        println(String.format("\tABS-X = %s (12.0,0.0,70.0) [90,90,0]", ForwardSolver.computePositionDescription(Joint.ABS_X.name)))
        println(String.format("\tABS-Z = %s (0.0,0.0,121.6) [0,90,90]", ForwardSolver.computePositionDescription(Joint.ABS_Z.name)))
        println(String.format("\tCHEST_Y = %s (2.8,0.0,201.5) [0,0,0]", ForwardSolver.computePositionDescription(Joint.CHEST_Y.name)))
        println(String.format("\tCHEST_X = %s (2.8,0.0,193.5) [90,90,0]", ForwardSolver.computePositionDescription(Joint.CHEST_X.name)))
        println(String.format("\tNECK_Z = %s  (7.8,0.0,277.5) [0,90,90]", ForwardSolver.computePositionDescription(Joint.NECK_Z.name)))
        println(String.format("\tNECK_Y = %s (27.8,0.0,297.5) [0,0,0]", ForwardSolver.computePositionDescription(Joint.NECK_Y.name)))
        println(String.format("\tNOSE   = %s (42.8, 0.0, 337.5)[0,0,0]", ForwardSolver.computePositionDescription(Appendage.NOSE.name)))
        println(String.format("\tLEFT_EAR  = %s (-2.2, 62.0,  352.5)[90,90,0]", ForwardSolver.computePositionDescription(Appendage.LEFT_EAR.name)))
        println(String.format("\tRIGHT_EAR = %s (-2.2, -62.0, 352.5)[90,90,180]", ForwardSolver.computePositionDescription(Appendage.RIGHT_EAR.name)))
        println(String.format("\tLEFT_EYE  = %s (27.8, 32.0,  352.5)[0,0,0]", ForwardSolver.computePositionDescription(Appendage.LEFT_EYE.name)))
        println(String.format("\tRIGHT_EYE = %s (27.8, -32.0, 352.5)[0,0,0]", ForwardSolver.computePositionDescription(Appendage.RIGHT_EYE.name)))
        println("        BACK JOINTS/APPENDAGES VERIFIED ####################")
        println("   ######## VERIFIED to here ####################")
        println("======== Test Joints along upper body sides - home pose")
        println(String.format("\tRIGHT_SHOULDER_Y = %s [0,0,0]", ForwardSolver.computePositionDescription(Joint.RIGHT_SHOULDER_Y.name)))
        println(String.format("\tLEFT_SHOULDER_Y  = %s [0,0,0]", ForwardSolver.computePositionDescription(Joint.LEFT_SHOULDER_Y.name)))
        println(String.format("\tRIGHT_SHOULDER_X = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_SHOULDER_X.name)))
        println(String.format("\tLEFT_SHOULDER_X  = %s (6.8, 105.5, 243.5)", ForwardSolver.computePositionDescription(Joint.LEFT_SHOULDER_X.name)))
        println(String.format("\tRIGHT_SHOULDER_Z = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_SHOULDER_Z.name)))
        println(String.format("\tLEFT_SHOULDER_Z  = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_SHOULDER_Z.name)))
        println(String.format("\tRIGHT_ELBOW_Y    = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_ELBOW_Y.name)))
        println(String.format("\tLEFT_ELBOW_Y     = %s (-3.2, 141.8, 143.8)", ForwardSolver.computePositionDescription(Joint.LEFT_ELBOW_Y.name)))
        println(String.format("\tRIGHT_FINGER = %s ", ForwardSolver.computePositionDescription(Appendage.RIGHT_FINGER.name)))
        println(String.format("\tLEFT_FINGER  = %s ", ForwardSolver.computePositionDescription(Appendage.LEFT_FINGER.name)))
        println("         UPPER JOINTS/APPENDAGES VERIFIED ####################")
        println("======== Test Joints along lower body sides - home position")
        println(String.format("\tRIGHT_HIP_X = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_HIP_X.name)))
        println(String.format("\tLEFT_HIP_X  = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_HIP_X.name)))
        println(String.format("\tRIGHT_HIP_Z = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_HIP_Z.name)))
        println(String.format("\tLEFT_HIP_Z  = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_HIP_Z.name)))
        println(String.format("\tRIGHT_HIP_Y = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_HIP_Y.name)))
        println(String.format("\tLEFT_HIP_Y  = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_HIP_Y.name)))
        println(String.format("\tRIGHT_KNEE_Y = %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_KNEE_Y.name)))
        println(String.format("\tLEFT_KNEE_Y  = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_KNEE_Y.name)))
        println(String.format("\tRIGHT_ANKLE_Y= %s ", ForwardSolver.computePositionDescription(Joint.RIGHT_ANKLE_Y.name)))
        println(String.format("\tLEFT_ANKLE_Y = %s ", ForwardSolver.computePositionDescription(Joint.LEFT_ANKLE_Y.name)))
        println(String.format("\tRIGHT_HEEL = %s ", ForwardSolver.computePositionDescription(Appendage.RIGHT_HEEL.name)))
        println(String.format("\tLEFT_HEEL  = %s ", ForwardSolver.computePositionDescription(Appendage.LEFT_HEEL.name)))
        println(String.format("\tRIGHT_TOE  = %s ", ForwardSolver.computePositionDescription(Appendage.RIGHT_TOE.name)))
        println(String.format("\tLEFT_TOE  = %s ", ForwardSolver.computePositionDescription(Appendage.LEFT_TOE.name)))
        println("         LOWER JOINTS/APPENDAGES VERIFIED ####################")
      */
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

