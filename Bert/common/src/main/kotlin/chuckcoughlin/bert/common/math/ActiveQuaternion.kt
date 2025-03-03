/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * @See: https://blog.robotiq.com/how-to-calculate-a-robots-forward-kinematics-in-5-easy-steps
 */
package chuckcoughlin.bert.common.math

import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.MotorConfiguration
import chuckcoughlin.bert.common.model.RobotModel
import java.util.logging.Logger

/**
 * Extend the static Quaternion to include a joint. The joint is always
 * perpendicular to the source of the limb.
 */
class ActiveQuaternion( mcfg: MotorConfiguration ) : Quaternion() {
    val mc: MotorConfiguration

    private val CLSS = "ActiveQuaternion"
    /**
     */
    init {
        mc = mcfg
    }
}