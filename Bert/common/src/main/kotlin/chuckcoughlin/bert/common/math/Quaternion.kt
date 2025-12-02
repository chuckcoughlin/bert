/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * @See: https://blog.robotiq.com/how-to-calculate-a-robots-forward-kinematics-in-5-easy-steps
 */
package chuckcoughlin.bert.common.math

import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.Point3D
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.solver.JointTree
import java.util.logging.Logger
import kotlin.math.cos
import kotlin.math.sin

/**
 * Define a quaternion for the specific purpose of calculating the
 * forward kinematics of the robot end effectors. The data structure
 * is a 4x4 double array. Array order is [row][col]
 */
class Quaternion {
    var matrix: Array<DoubleArray>
    var roll: Array<DoubleArray>
    var pitch: Array<DoubleArray>
    var yaw: Array<DoubleArray>
    var translation: Array<DoubleArray>

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
    // X axis
    fun setRoll(angle:Double) {
        roll[1][1] = cos(angle)
        roll[1][2] = -sin(angle)
        roll[2][1] = sin(angle)
        roll[2][2] = cos(angle)
    }
    // Y axis
    fun setPitch(angle:Double) {
        //if(DEBUG) LOGGER.info(String.format("%s.setPitch: %2.2f",CLSS,angle*180.0/Math.PI))
        pitch[0][0] = cos(angle)
        pitch[0][2] = -sin(angle)
        pitch[2][0] = sin(angle)
        pitch[2][2] = cos(angle)
    }
    // Z axis
    fun setYaw(angle:Double) {
        yaw[0][0] = cos(angle)
        yaw[0][1] = -sin(angle)
        yaw[1][0] = sin(angle)
        yaw[1][1] = cos(angle)
    }
    fun setTranslation(x:Double,y:Double,z:Double) {
        //if(DEBUG) LOGGER.info(String.format("%s.setTranslation: %2.2f %2.2f %2.2f",CLSS,x,y,z))
        translation[0][3] = x
        translation[1][3] = y
        translation[2][3] = z
    }
    // Roll, pitch, yaw are in degrees. Convert to radians.
    // This refers to the orientation of the link with respect to the
    // previous link.
    fun setRpy(roll:Double,pitch:Double,yaw:Double) {
        setRoll(roll)
        setPitch(pitch)
        setYaw(yaw)
        update()
    }
    /**
     * Multiply two matrices (expressed as arrays of double arrays)
     * Square matrices, same size.
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
     * @return the magnitude of the axes of the rotation matrix
     */
    fun direction(): DoubleArray {
        val theta1 = Math.acos(matrix[0][0])
        val theta2 = Math.acos(matrix[1][1])
        val theta3 = Math.acos(matrix[2][2])
        return doubleArrayOf(theta1*180.0/Math.PI,theta2*180.0/Math.PI,theta3*180.0/Math.PI)
    }

    fun directionToText() : String {
        val dir = direction()
        return(String.format("%3.0f,%3.0f,%3.0f",dir[0],dir[1],dir[2]))
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
     * Update the quaternion matrix from prior settings of
     * rotation and position.
     * Lengths ~mm, angles ~ radians
     */
    fun update() {
        matrix = multiply(multiply(multiply(translation,roll),pitch),yaw)
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
    fun dump() : String {
        val n = matrix.size
        val buf = StringBuffer()
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


    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        matrix = arrayOf(         // Initialize as an empty matrix
            doubleArrayOf(0.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,0.0)
        )
        roll = arrayOf(         // For rotation around x axis
                doubleArrayOf(1.0,0.0,0.0,0.0),
                doubleArrayOf(0.0,1.0,0.0,0.0),
                doubleArrayOf(0.0,0.0,1.0,0.0),
                doubleArrayOf(0.0,0.0,0.0,1.0)
        )
        pitch = arrayOf(         // For rotation around y axis
                doubleArrayOf(1.0,0.0,0.0,0.0),
                doubleArrayOf(0.0,1.0,0.0,0.0),
                doubleArrayOf(0.0,0.0,1.0,0.0),
                doubleArrayOf(0.0,0.0,0.0,1.0)
        )
        yaw = arrayOf(         // For rotation around z axis
                doubleArrayOf(1.0,0.0,0.0,0.0),
                doubleArrayOf(0.0,1.0,0.0,0.0),
                doubleArrayOf(0.0,0.0,1.0,0.0),
                doubleArrayOf(0.0,0.0,0.0,1.0)
        )
        translation = arrayOf(         // Position without rotation
            doubleArrayOf(1.0,0.0,0.0,0.0),
            doubleArrayOf(0.0,1.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,1.0,0.0),
            doubleArrayOf(0.0,0.0,0.0,1.0)
        )
    }
}