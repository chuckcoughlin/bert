/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
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
 * All angles in radians.
 * matrix[row][col]
 */
class Rom {
    var roll: Array<DoubleArray>
    var pitch: Array<DoubleArray>
    var yaw: Array<DoubleArray>
    // X axis
    fun setRoll(phi:Double) {
        roll[0][0] = 1.0
        roll[0][1] = 0.0
        roll[0][2] = 0.0
        roll[1][0] = 0.0
        roll[1][1] = cos(phi)
        roll[1][2] = -sin(phi)
        roll[2][0] = 0.0
        roll[2][1] = sin(phi)
        roll[2][2] = cos(phi)
    }

    // y axis
    fun setPitch(theta:Double) {
        pitch[0][0] = cos(theta)
        pitch[0][1] = 0.0
        pitch[0][2] =sin(theta)
        pitch[1][0] = 0.0
        pitch[1][1] = 1.0
        pitch[1][2] = 0.0
        pitch[2][0] = -sin(theta)
        pitch[2][1] = 0.0
        pitch[2][2] = cos(theta)
    }

    // z axis
    fun setYaw(psi:Double) {
        yaw[0][0] = cos(psi)
        yaw[0][1] = sin(psi)
        yaw[0][2] = 0.0
        yaw[1][0] = sin(psi)
        yaw[1][1] = cos(psi)
        yaw[1][2] = 0.0
        yaw[2][0] = 0.0
        yaw[2][1] = 0.0
        yaw[2][2] = 1.0
    }

    fun clone() : Rom {
        val copy = Rom()
        copy.roll = roll.clone()
        copy.pitch = pitch.clone()
        copy.yaw = yaw.clone()
        return copy
    }
     fun dump(comment:String) : String {
        val n = roll.size
        val buf = StringBuffer()
        buf.append(comment)
        buf.append('\n')
        for(row in 0 until n) {
            for( col in 0 until n) {
                val value = roll[row][col]
                buf.append(value.toString())
                buf.append('\t')
            }
            buf.append('\n')
        }
         for(row in 0 until n) {
             for( col in 0 until n) {
                 val value = pitch[row][col]
                 buf.append(value.toString())
                 buf.append('\t')
             }
             buf.append('\n')
         }
         for(row in 0 until n) {
             for( col in 0 until n) {
                 val value = yaw[row][col]
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
        roll = arrayOf(         // Initialize as an empty matrix
            doubleArrayOf(0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0)
        )
        pitch = arrayOf(         // Initialize as an empty matrix
            doubleArrayOf(0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0)
        )
        yaw = arrayOf(         // Initialize as an empty matrix
            doubleArrayOf(0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0),
            doubleArrayOf(0.0,0.0,0.0)
        )
        setRoll(0.0)
        setPitch(0.0)
        setYaw(0.0)
    }
}