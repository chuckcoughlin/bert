/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.model.URDFModel.origin
import java.util.logging.Logger

/**
 * Internal Measurement Unit. This is the origin
 * of all chains. As for positioning, we allow
 * only rotations.
 * @param alpha angle of rotation forward and back
 * @param theta angle of rotation side to side
 */
object IMU {

    var axis: DoubleArray
    var alpha: Double
    var theta: Double
    val quaternion: Quaternion

     fun update() {
        quaternion.update(0.0,0.0,0.0,0.0)
    }

    private val CLSS = "IMU"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    init {
        DEBUG= RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        axis =doubleArrayOf(0.0, 0.0, 0.0)
        quaternion = Quaternion()
        alpha = 0.0
        theta = 0.0
    }
}