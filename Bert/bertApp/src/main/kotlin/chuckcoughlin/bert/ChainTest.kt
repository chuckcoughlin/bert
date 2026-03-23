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
        val tree = URDFModel.createJointTree()
        tree.setJointsToHome()
        tree.computeJointPositions()
        var q : Quaternion

        // Print some JSON
        println("======== joints ========")
        var json = tree.jointCoordinatesToJson()
        println(json)
        println("======== links =======")
        json = tree.skeletonToJson()
        println(json)
        /*
        println("======== LinkSequence (verified) =======")
        for(link in tree.linkSequence) {
            println(String.format("\t%s ", link.endJoint.name))
        }
        */

        // Test the links to some extremities
        println(String.format("==================== %s ===========================================",CLSS ))

        println("======== Test LEFT_EAR to PELVIS position-chain")
        var chain = tree.createLinkChain(Joint.LEFT_EAR)
        for (link in chain) {
            println(String.format("\t%s ", link.endJoint.name))
        }
        println("======== Test RIGHT_FINGER to PELVIS link-chain")
        chain = tree.createLinkChain(Joint.RIGHT_FINGER)
        for (link in chain) {
            println(String.format("\t%s ", link.endJoint.name))
        }
        println("======== Test ABS_X to PELVIS link-chain")
        chain = tree.createLinkChain(Joint.ABS_X)
        for (link in chain) {
            println(String.format("\t%s ", link.endJoint.name))
        }
        // Right-handed coordinate system: x positive to front, y positive to right, z positive up
        println("======== Test ABS_Y for IMU orientations (directions should match)")
        val root = tree.getOrCreateJointPosition(Joint.IMU)   // IMU
        root.setOrientation(0.0,0.0,0.0)
        var jp = tree.updateJointPosition(Joint.ABS_Y)
        println(String.format("Root oriention [0,0,0]    : ABSY = (%s|%s)",jp.positionToText(),jp.orientationToText()))
        root.setOrientation(90.0,0.0,0.0)
        jp = tree.updateJointPosition(Joint.ABS_Y)
        // ans: 12,-62, 0 | 0,90,90  VERIFIED
        println(String.format("Root oriention [90,0,0]   : ABSY = (%s|%s)",jp.positionToText(),jp.orientationToText()))
        root.setOrientation(0.0,90.0,0.0)
        jp = tree.updateJointPosition(Joint.ABS_Y)
        // ans:  62, 0,-12 | 90,0,90 VERIFIED
        println(String.format("Root oriention [0,90,0]   : ABSY = (%s|%s)",jp.positionToText(),jp.orientationToText()))
        root.setOrientation(0.0,0.0,90.0)
        jp = tree.updateJointPosition(Joint.ABS_Y)
        // ans: 0, 12,62 | 90,90,0 VERIFIED
        println(String.format("Root oriention [0,0,90]   : ABSY = (%s|%s)",jp.positionToText(),jp.orientationToText()))

        println("======== Test Joints along back to head - home position")
        tree.setJointsToHome()
        jp = tree.updateJointPosition(Joint.ABS_Y)
        println(String.format("\t%s (12.0,0.0,62 | 0,0,0]     : (%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.ABS_X)
        println(String.format("\t%s (12.0,0.0,70.0) [90,90,0] : (%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.ABS_Z)
        println(String.format("\t%s (0.0,0.0,121.6) [0,90,90] : (%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.CHEST_Y)
        println(String.format("\t%s (2.8,0.0,201.5) [180,0,0] : (%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.CHEST_X)
        println(String.format("\t%s (2.8,0.0,193.5) [90,90,0] : (%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.NECK_Z)
        println(String.format("\t%s (7.8,0.0,277.5) [0,90,90]  : (%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.NECK_Y)
        println(String.format("\t%s (27.8,0.0,297.5) [0,0,0]   : (%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.NOSE)
        println(String.format("\t%s (42.8,0.0,337.5) [0,0,0]     : (%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_EAR)
        println(String.format("\t%s (57.8,62.0,352.5) [90,90,0]   : (%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.RIGHT_EAR)
        println(String.format("\t%s (57.8,-62.0,352.5) [90,90,180]: (%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_EYE)
        println(String.format("\t%s (27.8,32.0,352.5) [0,0,0]     : (%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.RIGHT_EYE)
        println(String.format("\t%s (27.8,-32.0,352.5) [0,0,0]   : (%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        println("        BACK JOINTS/APPENDAGES VERIFIED ####################")
        println("======== Test Joints along upper body sides - home pose")
        jp = tree.updateJointPosition(Joint.RIGHT_SHOULDER_Y)
        println(String.format("\t%s(6.8, 77.1,243.5) [0,0,180]:(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_SHOULDER_Y)
        println(String.format("\t%s (6.8,-77.1,243.5) [0,0,180]:(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.RIGHT_SHOULDER_X)
        println(String.format("\t%s(6.8, 105.5,243.5) [90,90,180]:(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_SHOULDER_X)
        println(String.format("\t%s (6.8,-105.5,243.5) [90,90,180]:(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.RIGHT_SHOULDER_Z)
        println(String.format("\t%s(-11.7, 105.5,160.5) [0,90,90]:(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_SHOULDER_Z)
        println(String.format("\t%s (-11.7,-105.5,160.5) [0,90,90]:(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.RIGHT_ELBOW_Y)
        println(String.format("\t%s (-21.7,105.5,79.3) [0,0,180]  :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_ELBOW_Y)
        println(String.format("\t%s (-21.7,-105.5,79.3) [0,0,180]  :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.RIGHT_FINGER)
        println(String.format("\t%s (-28.6,100.5,-41.7) [90,90,0]  :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_FINGER)
        println(String.format("\t%s (-28.6,-100.5,-41.7) [90,90,180] :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        println("         UPPER JOINTS/APPENDAGES VERIFIED ####################")
        println("======== Test Joints along lower body sides - home position")
        jp = tree.updateJointPosition(Joint.RIGHT_HIP_X)
        println(String.format("\t%s (0.0,22.5,0.0) [90,90,180]  :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_HIP_X)
        println(String.format("\t%s (0.0,-22.5,0.0) [90,90,180] :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.RIGHT_HIP_Z)
        println(String.format("\t%s (0.0,66.5,5.0) [180,90,90]  :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_HIP_Z)
        println(String.format("\t%s (0.0,-66.5,5.0) [180,90,90] :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.RIGHT_HIP_Y)
        println(String.format("\t%s (0.0,66.5,-19.0) [0,0,0]  :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_HIP_Y)
        println(String.format("\t%s (0.0,-66.5,-19.0) [0,0,0] :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.RIGHT_KNEE_Y)
        println(String.format("\t%s (0.0,66.5,-201.0) [0,0,0]  :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_KNEE_Y)
        println(String.format("\t%s (0.0,-66.5,-201.0) [0,0,0] :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.RIGHT_ANKLE_Y)
        println(String.format("\t%s (0.0,66.5,-381.0) [0,0,0]  :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_ANKLE_Y)
        println(String.format("\t%s (0.0,-66.5,-381.0) [0,0,0] :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.RIGHT_HEEL)
        println(String.format("\t%s (-43.0,78.5,-416.5) [0,0,0]  :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_HEEL)
        println(String.format("\t%s (-43.0,-78.5,-416.5) [0,0,0] :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.RIGHT_TOE)
        println(String.format("\t%s (95.0,51.5,-416.5) [0,0,0]  :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        jp = tree.updateJointPosition(Joint.LEFT_TOE)
        println(String.format("\t%s (95.0,-51.5,-416.5) [0,0,0] :(%s|%s)",jp.joint.name,jp.positionToText(),jp.orientationToText()))
        println("         LOWER JOINTS/APPENDAGES VERIFIED ####################")
        //println("==========================================================")
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
                else ->  {
                   // Not a joint
                }
            }
        }
    }
}

