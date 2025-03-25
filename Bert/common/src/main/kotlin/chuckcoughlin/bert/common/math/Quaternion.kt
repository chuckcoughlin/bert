/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * @See: https://blog.robotiq.com/how-to-calculate-a-robots-forward-kinematics-in-5-easy-steps
 */
package chuckcoughlin.bert.common.math

import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.Point3D
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.model.Solver
import java.util.logging.Logger

/**
 * Define a quaternion for the specific purpose of calculating the
 * forward kinematics of the robot end effectors. The main structure
 * is a 4-dimensional double array.
 */
open class Quaternion( ) {
    var matrix: Array<DoubleArray>

    fun multiplyBy(q: Quaternion): Quaternion {
        val result = Quaternion()
        result.matrix = multiplyBy(q.matrix)
        return result
    }

    private fun multiplyBy(mtx: Array<DoubleArray>): Array<DoubleArray> {
        val n = mtx.size
        if (n != matrix.size || mtx[0].size != n) {
            throw IllegalArgumentException(String.format("%s:multiplyBy: Matrices cannot be multiplied due to incompatible dimensions (%d vs %d).",
                CLSS, n, matrix.size))
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
     * The current orientation
     */
    fun orientation(): DoubleArray {
        var p = doubleArrayOf(matrix[0][0],matrix[0][1],matrix[0][2])
        return p
    }
    /**
     * The current position
     */
    fun position(): Point3D {
        var p = Point3D(matrix[3][0],matrix[3][1],matrix[3][2])
        return p
    }

    /**
     * Update the quaternion matrix from the object parameters.
     * @param  d offset to source along z ~mm
     * @param r distance to source along common normal ~ mm
     * @param alpha angle to double normal around z ~ derees
     * @param theta: angle of rotation of joint ~ degrees
     */
    fun update(d:Double,r:Double,alpha:Double,theta:Double) {
        if(DEBUG) LOGGER.info(String.format("%s.update: d=%2.2f, r= %2.2f, alpha = %2.2f, theta = %2.2f ",
               CLSS,d,r,alpha,theta))
        matrix[0][0] = Math.cos(theta*Math.PI/180.0)
        matrix[0][1] = -Math.sin(theta*Math.PI/180.0) * Math.cos(alpha*Math.PI/180.0)
        matrix[0][2] =  Math.sin(theta*Math.PI/180.0) * Math.sin(alpha*Math.PI/180.0)
        matrix[0][3] = r * Math.cos(theta*Math.PI/180.0)
        matrix[1][0] = Math.sin(theta*Math.PI/180.0)
        matrix[1][1] = Math.cos(theta*Math.PI/180.0)*Math.cos(alpha*Math.PI/180.0)
        matrix[1][2] = -Math.cos(theta*Math.PI/180.0)*Math.sin(alpha*Math.PI/180.0)
        matrix[1][3] = r * Math.sin(theta*Math.PI/180.0)
        matrix[2][0] = 0.0
        matrix[2][1] = Math.sin(alpha*Math.PI/180.0)
        matrix[2][2] = Math.cos(alpha*Math.PI/180.0)
        matrix[2][3] = d
        matrix[3][0] = 0.0
        matrix[3][1] = 0.0
        matrix[3][2] = 0.0
        matrix[3][3] = 1.0
    }

    companion object {
        /**
         * @return a new quaternion, the identity matrix.
         *    It just so happens that we initialize to
         *    an identity matrix by default.
         */
        fun identity(): Quaternion {
            val q = Quaternion()
            q.matrix = arrayOf(         // Initialize as identity matrix
                doubleArrayOf(1.0,0.0,0.0,0.0),
                doubleArrayOf(0.0,1.0,0.0,0.0),
                doubleArrayOf(0.0,0.0,1.0,0.0),
                doubleArrayOf(0.0,0.0,0.0,1.0)
            )
            return q
        }
    }
    private val CLSS = "Quaternion"
    val LOGGER = Logger.getLogger(CLSS)
    val DEBUG: Boolean
    val DIM = 4

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        matrix = arrayOf(         // Initialize as an empty matrix
            doubleArrayOf(0.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,0.0)
        )
    }
}