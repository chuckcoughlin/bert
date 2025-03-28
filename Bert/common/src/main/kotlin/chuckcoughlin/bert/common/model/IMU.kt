/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import java.util.logging.Logger

/**
 * Internal Measurement Unit. This is the origin
 * of all chains.
 */
object IMU {
    var origin: Point3D
    var axis: DoubleArray   = doubleArrayOf(0.0, 0.0, 0.0)
    val quaternion: Quaternion

    fun updateQuaternium() {
        quaternion.update(origin.z,origin.x,0.0,0.0)
    }

    private val CLSS = "IMU"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    init {
        DEBUG= RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        axis   = doubleArrayOf(0.0, 0.0, 0.0)
        origin = Point3D(0.0, 0.0, 0.0)
        quaternion = Quaternion()
    }
}