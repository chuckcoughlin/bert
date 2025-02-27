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

        if (n != q.size || matrix[0].size != n ) {
            throw IllegalArgumentException(String.format("%s:multiplyBy: Matrices cannot be multiplied due to incompatible dimensions (%d vs %d).",
                 CLSS,n,q.size))
        }

        val result = Array(n) { DoubleArray(n) }

        for (i in 0 until n) {
            for (j in 0 until n) {
                for (k in 0 until n) {
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
        q = arrayOf(
            doubleArrayOf(0.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,1.0)
        )
    }
}