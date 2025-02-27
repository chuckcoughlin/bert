/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * @See: https://blog.robotiq.com/how-to-calculate-a-robots-forward-kinematics-in-5-easy-steps
 */
package chuckcoughlin.bert.common.util

import chuckcoughlin.bert.common.model.Appendage
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.LinkPin
import chuckcoughlin.bert.common.model.PinType
import chuckcoughlin.bert.common.model.Point3D
import chuckcoughlin.bert.common.model.RobotModel
import org.hipparchus.complex.Quaternion
import java.util.logging.Logger

/**
 * Define a quaternion for the specific purpose of calculating the
 * forward kinematics of the robot end effectors. The main structure
 * is a 4-dimensional double array.
 */
class Quaternion( ) {
    val q: Array<DoubleArray>

    fun multiplyBy(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val n = matrix.size
        val m = matrix[0].size
        val p = matrix[0].size

        if (m != q.size) {
            throw IllegalArgumentException(String.format("%s:multiplyBy: Matrices cannot be multiplied due to incompatible dimensions (%d vs %d).",
                 CLSS,m,q.size))
        }

        val result = Array(n) { DoubleArray(p) }

        for (i in 0 until n) {
            for (j in 0 until p) {
                for (k in 0 until m) {
                    result[i][j] += matrix[i][k] * q[k][j]
                }
            }
        }
        return result
    }

    private val CLSS = "Quaternion"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean
    private val DIM = 4
    /**
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        q = Array(DIM) { DoubleArray(DIM) }
        q[0] = doubleArrayOf(0.0,0.0,0.0,0.0)
        q[1] = doubleArrayOf(0.0,0.0,0.0,0.0)
        q[2] = doubleArrayOf(0.0,0.0,0.0,0.0)
        q[2] = doubleArrayOf(0.0,0.0,0.0,0.0)
    }
}