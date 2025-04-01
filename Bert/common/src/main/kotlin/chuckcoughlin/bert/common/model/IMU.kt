/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import java.util.logging.Logger

/**
 * Internal Measurement Unit. This is the origin
 * of all chains. If we update the dimensions,
 * we must update the quaternium accordingly.
 */
object IMU {
    var orientation =doubleArrayOf(0.0, 0.0, 0.0)
        get() = field
        set(arr) {
            updateQuaternium()
            field = arr
        }
    var origin = Point3D(0.0, 0.0, 0.0)
        get() = field
        set(arr) {
            updateQuaternium()
            field = arr
        }
    val quaternion: Quaternion

    private fun updateQuaternium() {
        quaternion.update(origin.z,origin.x,0.0,0.0)
    }

    private val CLSS = "IMU"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    init {
        DEBUG= RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        quaternion = Quaternion()
        updateQuaternium()
    }
}