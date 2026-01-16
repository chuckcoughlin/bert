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
 * Convert Euler angles into 3x3 rotation matrices.
 * matrix[row][col]
 */
class Rom {
    var matrix: Array<DoubleArray>
    // X axis
    fun setRoll(phi:Double) {
        matrix[0][0] = 1.0
        matrix[0][1] = 0.0
        matrix[0][2] = 0.0
        matrix[1][0] = 0.0
        matrix[1][1] = cos(phi)
        matrix[1][2] = -sin(phi)
        matrix[2][0] = 0.0
        matrix[2][1] = sin(phi)
        matrix[2][2] = cos(phi)
    }

    // y axis
    fun setPitch(theta:Double) {
        matrix[0][0] = cos(theta)
        matrix[0][1] = 0.0
        matrix[0][2] =sin(theta)
        matrix[1][0] = 0.0
        matrix[1][1] = 1.0
        matrix[1][2] = 0.0
        matrix[2][0] = -sin(theta)
        matrix[2][1] = 0.0
        matrix[2][2] = cos(theta)
    }

    // z axis
    fun setYaw(psi:Double) {
        matrix[0][0] = cos(psi)
        matrix[0][1] = sin(psi)
        matrix[0][2] = 0.0
        matrix[1][0] = sin(psi)
        matrix[1][1] = cos(psi)
        matrix[1][2] = 0.0
        matrix[2][0] = 0.0
        matrix[2][1] = 0.0
        matrix[2][2] = 1.0
    }

    fun clone() : Rom {
        val copy = Rom()
        copy.matrix = matrix.clone()
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

    private val CLSS = "Rom"
    val LOGGER = Logger.getLogger(CLSS)
    val DEBUG: Boolean


    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
        matrix = arrayOf(         // Initialize as an empty matrix
            doubleArrayOf(0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0)
        )
    }
}