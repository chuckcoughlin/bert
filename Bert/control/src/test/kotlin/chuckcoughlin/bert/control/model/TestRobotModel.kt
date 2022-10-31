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
class TestRobotModel(configPath: Path?) : AbstractRobotModel(configPath) {
    /**
     * No need for controllers in our test
     */
    fun analyzeControllers() {}

    /**
     * Analyze the document. The information retained is dependent on the context
     * (client or server). This must be called before the model is accessed.
     */
    fun populate() {
        analyzeProperties()
        analyzeMotors()
        initializeMotorConfigurations()
    }

    /**
     * Set the initial positions of the motors to "home"!
     */
    private fun initializeMotorConfigurations() {
        for (mc in getMotors().values()) {
            // Set some reasonable values from the "home" pose.
            when (mc.getJoint()) {
                Joint.ABS_X -> mc.setPosition(180.0)
                Joint.ABS_Y -> mc.setPosition(180.0)
                Joint.ABS_Z -> mc.setPosition(0.0)
                Joint.BUST_X -> mc.setPosition(180.0)
                Joint.BUST_Y -> mc.setPosition(180.0)
                Joint.NECK_Y -> mc.setPosition(0.0)
                Joint.NECK_Z -> mc.setPosition(0.0)
                Joint.LEFT_ANKLE_Y -> mc.setPosition(90.0)
                Joint.LEFT_ARM_Z -> mc.setPosition(0.0)
                Joint.LEFT_ELBOW_Y -> mc.setPosition(180.0)
                Joint.LEFT_HIP_X -> mc.setPosition(180.0)
                Joint.LEFT_HIP_Y -> mc.setPosition(180.0)
                Joint.LEFT_HIP_Z -> mc.setPosition(0.0)
                Joint.LEFT_KNEE_Y -> mc.setPosition(180.0)
                Joint.LEFT_SHOULDER_X -> mc.setPosition(180.0)
                Joint.LEFT_SHOULDER_Y -> mc.setPosition(180.0)
                Joint.RIGHT_ANKLE_Y -> mc.setPosition(90.0)
                Joint.RIGHT_ARM_Z -> mc.setPosition(0.0)
                Joint.RIGHT_ELBOW_Y -> mc.setPosition(180.0)
                Joint.RIGHT_HIP_X -> mc.setPosition(180.0)
                Joint.RIGHT_HIP_Y -> mc.setPosition(180.0)
                Joint.RIGHT_HIP_Z -> mc.setPosition(0.0)
                Joint.RIGHT_KNEE_Y -> mc.setPosition(180.0)
                Joint.RIGHT_SHOULDER_X -> mc.setPosition(180.0)
                Joint.RIGHT_SHOULDER_Y -> mc.setPosition(180.0)
            }
        }
    }

    companion object {
        private const val CLSS = "TestRobotModel"
        private val LOGGER = Logger.getLogger(CLSS)
    }
}