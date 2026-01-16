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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Define a quaternion for the specific purpose of calculating the
 * forward kinematics of the robot end effectors. The data structure
 * is a 4x4 double array. Array order is [row][col]. All angles are
 * expressed in radians.
 */
class Quaternion {
    var matrix: Array<DoubleArray>
    var roll:  Rom
    var pitch: Rom
    var yaw:   Rom
    var translation: DoubleArray

    /**
     * @return the current orientation with respect to the reference frame ~ radians.
     */
    fun direction(): DoubleArray {
        val theta1 = Math.acos(matrix[0][0])
        val theta2 = Math.acos(matrix[1][1])
        val theta3 = Math.acos(matrix[2][2])
        return doubleArrayOf(theta1,theta2,theta3)
    }

    /**
     * @return a string representation of the current orientation with respect to the reference frame ~ degrees.
     */
    fun directionToText() : String {
        val dir = direction()
        return(String.format("%3.0f,%3.0f,%3.0f",dir[0]*180.0/Math.PI,dir[1]*180.0/Math.PI,dir[2]*180.0/Math.PI))
    }


    /**
     * Insert a 3x3 rotation matrix into the quaternion matrix.
     * matrix[row][col]
     */
    fun insertRotation(r:Array<DoubleArray>) {
        matrix[0][0] = r[0][0]
        matrix[0][1] = r[0][1]
        matrix[0][2] = r[0][2]
        matrix[1][0] = r[1][0]
        matrix[1][1] = r[1][1]
        matrix[1][2] = r[1][2]
        matrix[2][0] = r[2][0]
        matrix[2][1] = r[2][1]
        matrix[2][2] = r[2][2]
    }

    fun insertTranslation(t:DoubleArray) {
        matrix[0][3] = t[0]
        matrix[1][3] = t[1]
        matrix[2][3] = t[2]
    }
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
     * The current position in the parent frame (x,y,z)
     */
    fun position(): Point3D {
        return(Point3D(matrix[0][3],matrix[1][3],matrix[2][3]))
    }

    fun positionToText() : String {
        val pos = position()
        return(String.format("%3.1f,%3.1f,%3.1f",pos.x,pos.y,pos.z))
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
    // X axis
    fun setRoll(phi:Double) {
        roll.setRoll(phi)
    }
    // Y axis
    fun setPitch(theta:Double) {
        pitch.setPitch(theta)
    }
    // Z axis
    fun setYaw(psi:Double) {
        yaw.setYaw(psi)
    }
    fun setTranslation(x:Double,y:Double,z:Double) {
        translation[0] = x
        translation[1] = y
        translation[2] = z
    }

    /**
     * Multiply two matrices (expressed as arrays of double arrays)
     * Square matrices, same size.
     */
    private fun multiply(a: Array<DoubleArray>,b: Array<DoubleArray>): Array<DoubleArray> {
        val n = a.size
        if (n != b.size || b[0].size != n) {
            throw IllegalArgumentException(String.format("%s:multiply: Matrices must havee same dimensions (%d vs %d).",
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
     * Multiply a matrix by a vector
     * Square matrix, of same size as the vector.
     */
    private fun multiply(a: Array<DoubleArray>,v: DoubleArray): Array<DoubleArray> {
        val n = a.size
        if (n != v.size ) {
            throw IllegalArgumentException(String.format("%s:multiply: Matrix and vector must have same sizes (%d vs %d).",
                CLSS, a.size, v.size))
        }
        val result = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            var sum = 0.0
            for (j in 0 until n) {
                for (k in 0 until n) {
                    sum += a[i][j] * v[j]
                    result[i][j] = sum
                }
            }
        }
        return result
    }

    /**
     * Multiply a vector by a matrix
     * Square matrix, of same size as the vector.
     */
    private fun multiply(v: DoubleArray,a: Array<DoubleArray>): DoubleArray {
        val n = a.size
        if (n != v.size ) {
            throw IllegalArgumentException(String.format("%s:multiply: Vector and matrix must have same sizes (%d vs %d).",
                CLSS, a.size, v.size))
        }
        val result = DoubleArray(n)
        for (i in 0 until n) {
            var sum = 0.0
            for (j in 0 until n) {
                for (k in 0 until n) {
                    sum += v[j] * a[j][i]
                    result[j] = sum
                }
            }
        }
        return result
    }
    /**
     * Perform a rotation operation, updating the internal matrix.
     */
    fun rotate() {
        val rotation = multiply(multiply(roll.matrix,pitch.matrix),yaw.matrix)
        insertRotation(rotation)
        val t = multiply(translation,matrix)
        insertTranslation(t)
    }

    /**
     * Update the quaternion matrix from prior settings of
     * rotation and position. (Order of multipllication = roll, pitch, yaw).
     * Lengths ~mm, angles ~ radians
     */
    fun update() {
        val rotation = multiply(multiply(roll.matrix,pitch.matrix),yaw.matrix)
        insertRotation(rotation)

    }

    fun clone() : Quaternion {
        val copy = Quaternion()
        copy.matrix = matrix.clone()
        copy.roll   = roll.clone()
        copy.pitch  = pitch.clone()
        copy.yaw    = yaw.clone()
        copy.translation = translation.clone()
        return copy
    }
     fun dump(comment:String) : String {
        val n = matrix.size
        val buf = StringBuffer()
        buf.append(comment)
        buf.append('\n')
        for(row in 0 until n) {
            for( col in 0 until n) {
                val value = matrix[row][col]
                buf.append(value.toString())
                buf.append('\t')
            }
            buf.append('\n')
        }
        return buf.toString()
    }

    fun logdetails(comment:String) {
        LOGGER.info(String.format("%s.logdetails =============== %s ==============",CLSS,comment))
        LOGGER.info(dumpmatrix("roll",roll.matrix))
        LOGGER.info(dumpmatrix("pitch",pitch.matrix))
        LOGGER.info(dumpmatrix("yaw",yaw.matrix))
        LOGGER.info(dumpmatrix("matrix",matrix))
        LOGGER.info(dump(comment))
    }

    private fun dumpmatrix(comment:String,m:Array<DoubleArray>) : String {
        val n = m.size
        val buf = StringBuffer()
        buf.append(comment)
        buf.append('\n')
        for(row in 0 until n) {
            for( col in 0 until n) {
                val value = m[row][col]
                buf.append(value.toString())
                buf.append('\t')
            }
            buf.append('\n')
        }
        return buf.toString()
    }

    companion object {
        /**
         * @return a new quaternion, the identity matrix.
         *    It just so happens that we initialize to
         *    an identity matrix by default.
         */
        fun identity(): Quaternion {
            val q = Quaternion()
            q.matrix = identityMatrix()
            return q
        }
        fun identityMatrix(): Array<DoubleArray> {
            val m = arrayOf(         // Initialize as identity matrix
                doubleArrayOf(1.0,0.0,0.0,0.0),
                doubleArrayOf(0.0,1.0,0.0,0.0),
                doubleArrayOf(0.0,0.0,1.0,0.0),
                doubleArrayOf(0.0,0.0,0.0,1.0)
            )
            return m
        }
    }
    private val CLSS = "Quaternion"
    val LOGGER = Logger.getLogger(CLSS)
    val DEBUG: Boolean


    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        matrix = arrayOf(         // Initialize as an empty matrix
            doubleArrayOf(0.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,0.0)
        )
        roll  = Rom()
        pitch = Rom()
        yaw   = Rom()
        translation = doubleArrayOf(0.0,0.0,0.0)
    }
}