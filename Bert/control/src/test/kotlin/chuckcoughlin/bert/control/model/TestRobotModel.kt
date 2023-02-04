/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.control.model

import chuckcoughlin.bert.common.model.AbstractRobotModel
import chuckcoughlin.bert.common.model.Joint
import java.nio.file.Path
import java.util.logging.Logger

/**
 * This class is used exclusively for testing.
 */
class TestRobotModel(configPath: Path) : AbstractRobotModel(configPath) {
    /**
     * No need for controllers in our test
     */
    override fun analyzeControllers() {}

    /**
     * Analyze the document. The information retained is dependent on the context
     * (client or server). This must be called before the model is accessed.
     */
    override fun populate() {
        analyzeProperties()
        analyzeMotors()
        initializeMotorConfigurations()
    }

    /**
     * Set the initial positions of the motors to "home"!
     */
    private fun initializeMotorConfigurations() {
        for (joint in motors.keys) {
            val mc = motors.get(joint)
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


    private val CLSS = "TestRobotModel"
    private val LOGGER = Logger.getLogger(CLSS)
}