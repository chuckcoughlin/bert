/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * @See: https://blog.robotiq.com/how-to-calculate-a-robots-forward-kinematics-in-5-easy-steps
 */
package chuckcoughlin.bert.common.math

import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.RobotModel
import java.util.logging.Logger

/**
 * Define a quaternion for the specific purpose of calculating the
 * forward kinematics of the robot end effectors. The main structure
 * is a 4-dimensional double array.
 */
open class Quaternion( ) {
    var matrix: Array<DoubleArray>
    var d: Double
    var alpha: Double
    var r: Double
    var theta: Double
    var offset:Double

    fun multiplyBy(q:Quaternion): Quaternion {
        val result = Quaternion()
        result.matrix = multiplyBy(q.matrix)
        return result
    }

    private fun multiplyBy(mtx: Array<DoubleArray>): Array<DoubleArray> {
        val n = mtx.size
        if (n != matrix.size || mtx[0].size != n ) {
            throw IllegalArgumentException(String.format("%s:multiplyBy: Matrices cannot be multiplied due to incompatible dimensions (%d vs %d).",
                 CLSS,n,matrix.size))
        }

        val result = Array(n) { DoubleArray(n) }

        for (i in 0 until n) {
            for (j in 0 until n) {
                for (k in 0 until n) {
                    result[i][j] += matrix[i][k] * mtx[k][j]
                }
            }
        }
        return result
    }

    /**
     * Create the quaternion matrix from the object parameters.
     * There are computations involved, so this is difficult to
     * do "real-time".
     */
    fun refresh() {
        matrix[0][0] = Math.cos(theta*Math.PI/180.0)
        matrix[2][3] = d
    }

    companion object {
        /**
         * @return a new quaternion, the identity matrix.
         *    It just so happens that we initialize to
         *    an identity matrix by default.
         */
        fun identity(): Quaternion {
            val q = Quaternion()
            return q
        }
    }
    private val CLSS = "Quaternion"
    val LOGGER = Logger.getLogger(CLSS)
    val DEBUG: Boolean
    val DIM = 4

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        matrix = arrayOf(         // Initialize as identity matrix
            doubleArrayOf(1.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,1.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,1.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,1.0)
        )
        d = 0.0
        r = 0.0
        alpha = 0.0
        theta = 0.0
        offset = 0.0
    }
}