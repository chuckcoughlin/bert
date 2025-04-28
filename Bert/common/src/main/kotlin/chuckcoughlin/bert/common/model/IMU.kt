/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Axis
import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.model.URDFModel.origin
import java.util.logging.Logger

/**
 * Internal Measurement Unit. This is the origin
 * of all chains. As for positioning, we allow
 * only rotations.
 */
object IMU {

    var axis: Axis
    val quaternion: Quaternion

    // Incorporate any rotations that may have been set
     fun update() {
         quaternion.update()
    }

    private val CLSS = "IMU"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    init {
        DEBUG= RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        axis = Axis.Y
        quaternion = Quaternion()
    }
}