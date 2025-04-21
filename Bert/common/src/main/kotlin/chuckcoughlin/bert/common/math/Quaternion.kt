/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * @See: https://blog.robotiq.com/how-to-calculate-a-robots-forward-kinematics-in-5-easy-steps
 */
package chuckcoughlin.bert.common.math

import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.Point3D
import chuckcoughlin.bert.common.model.RobotModel
import java.util.logging.Logger
import javax.swing.text.html.HTML.Tag.P

/**
 * Define a quaternion for the specific purpose of calculating the
 * forward kinematics of the robot end effectors. The data structure
 * is a 4x4 double array.
 */
open class Quaternion( ) {
    var matrix: Array<DoubleArray>

    /**
     * Multiply the given quaternion by the current
     * and return the result
     */
    fun preMultiplyBy(q: Quaternion): Quaternion {
        val result = Quaternion()
        result.matrix = multiply(q.matrix,this.matrix)
        return result
    }

    /**
     * Multiply the current quaternion by a
     * specified one. Return the result.
     */
    fun postMultiplyBy(q: Quaternion): Quaternion {
        val result = Quaternion()
        result.matrix = multiply(this.matrix,q.matrix)
        return result
    }

    /**
     * Multiply two matrices (expressed as arrays of double arrays)
     */
    private fun multiply(a: Array<DoubleArray>,b: Array<DoubleArray>): Array<DoubleArray> {
        val n = a.size
        if (n != b.size || b[0].size != n) {
            throw IllegalArgumentException(String.format("%s:multiplyBy: Matrices cannot be multiplied due to incompatible dimensions (%d vs %d).",
                CLSS, a.size, b.size))
        }

        val result = Array(n) { DoubleArray(n) }

        for (i in 0 until n) {
            for (j in 0 until n) {
                for (k in 0 until n) {
                    result[i][j] += a[i][k] * b[k][j]
                }
            }
        }
        return result
    }

    /**
     * The current orientation with respect to the reference frame.
     * @return alpha, theta
     */
    fun direction(): DoubleArray {
        val alpha = Math.acos(matrix[2][2])
        val theta = Math.acos(matrix[0][0])
        return doubleArrayOf(alpha*180.0/Math.PI,theta*180.0/Math.PI)
    }

    fun directionToText() : String {
        val dir = direction()
        return(String.format("%3.3f,%3.3f",dir[0],dir[1]))
    }
    /**
     * The current position in the parent frame (x,y,z)
     */
    fun position(): Point3D {
        return(Point3D(matrix[3][1],matrix[3][2],matrix[3][0]))
    }

    fun positionToText() : String {
        val pos = position()
        return(String.format("%3.3f,%3.3f,%3.3f",pos.x,pos.y,pos.z))
    }

    /**
     * Update the quaternion matrix from the object parameters.
     * Lengths ~mm, angles ~ radians
     * @param  d offset to source along z
     * @param r distance to source along common normal
     * @param alpha angle around common normal between source and current z
     * @param theta: angle of rotation of joint
     */
    fun update(d:Double,r:Double,alpha:Double,theta:Double) {
        if(DEBUG) LOGGER.info(String.format("%s.update: d=%2.2f, r= %2.2f, alpha = %2.2f, theta = %2.2f ",
               CLSS,d,r,alpha*180.0/Math.PI,theta*180.0/Math.PI))
        matrix[0][0] = Math.cos(theta)
        matrix[1][0] = -Math.sin(theta) * Math.cos(alpha)
        matrix[2][0] =  Math.sin(theta) * Math.sin(alpha)
        matrix[3][0] = r * Math.cos(theta)
        matrix[0][1] = Math.sin(theta)
        matrix[1][1] = Math.cos(theta)*Math.cos(alpha)
        matrix[2][1] = -Math.cos(theta)*Math.sin(alpha)
        matrix[3][1] = r * Math.sin(theta)
        matrix[0][2] = 0.0
        matrix[1][2] = Math.sin(alpha)
        matrix[2][2] = Math.cos(alpha)
        matrix[3][2] = d
        matrix[0][3] = 0.0
        matrix[1][3] = 0.0
        matrix[2][3] = 0.0
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