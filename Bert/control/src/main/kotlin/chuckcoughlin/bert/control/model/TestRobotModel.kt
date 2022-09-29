/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.control.model

import bert.share.model.AbstractRobotModel
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
                ABS_X -> mc.setPosition(180.0)
                ABS_Y -> mc.setPosition(180.0)
                ABS_Z -> mc.setPosition(0.0)
                BUST_X -> mc.setPosition(180.0)
                BUST_Y -> mc.setPosition(180.0)
                NECK_Y -> mc.setPosition(0.0)
                NECK_Z -> mc.setPosition(0.0)
                LEFT_ANKLE_Y -> mc.setPosition(90.0)
                LEFT_ARM_Z -> mc.setPosition(0.0)
                LEFT_ELBOW_Y -> mc.setPosition(180.0)
                LEFT_HIP_X -> mc.setPosition(180.0)
                LEFT_HIP_Y -> mc.setPosition(180.0)
                LEFT_HIP_Z -> mc.setPosition(0.0)
                LEFT_KNEE_Y -> mc.setPosition(180.0)
                LEFT_SHOULDER_X -> mc.setPosition(180.0)
                LEFT_SHOULDER_Y -> mc.setPosition(180.0)
                RIGHT_ANKLE_Y -> mc.setPosition(90.0)
                RIGHT_ARM_Z -> mc.setPosition(0.0)
                RIGHT_ELBOW_Y -> mc.setPosition(180.0)
                RIGHT_HIP_X -> mc.setPosition(180.0)
                RIGHT_HIP_Y -> mc.setPosition(180.0)
                RIGHT_HIP_Z -> mc.setPosition(0.0)
                RIGHT_KNEE_Y -> mc.setPosition(180.0)
                RIGHT_SHOULDER_X -> mc.setPosition(180.0)
                RIGHT_SHOULDER_Y -> mc.setPosition(180.0)
                UNKNOWN -> mc.setPosition(0.0)
            }
        }
    }

    companion object {
        private const val CLSS = "TestRobotModel"
        private val LOGGER = Logger.getLogger(CLSS)
    }
}