/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 * @See: https://blog.robotiq.com/how-to-calculate-a-robots-forward-kinematics-in-5-easy-steps
 */
package chuckcoughlin.bert.common.math

import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.JointLink
import chuckcoughlin.bert.common.model.JointPosition
import chuckcoughlin.bert.common.model.Point3D
import chuckcoughlin.bert.common.model.RobotModel
import java.util.logging.Logger

/**
 * Define a quaternion for the specific purpose of calculating the
 * forward kinematics of the robot end effectors. The data structure
 * is a 4x4 double array. Array order is [row][col]. All angles are
 * expressed in radians.
 */
class Quaternion () {
    var matrix: Array<DoubleArray>

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

    /**
     * Insert a 3x3 rotation matrix into the quaternion matrix.
     * matrix[row][col]
     */
    private fun insertRotation(r:Array<DoubleArray>) {
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

    private fun insertTranslation(t:DoubleArray) {
        matrix[0][3] = t[0]
        matrix[1][3] = t[1]
        matrix[2][3] = t[2]
    }

    fun clone() : Quaternion {
        val copy = Quaternion()
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

    fun logdetails(rom:Rom,comment:String) {
        LOGGER.info(String.format("%s.logdetails =============== %s ==============",CLSS,comment))
        LOGGER.info(dumpmatrix("roll",rom.roll))
        LOGGER.info(dumpmatrix("pitch",rom.pitch))
        LOGGER.info(dumpmatrix("yaw",rom.yaw))
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
        /**
         * @return a quaternion for a simple rotation around the
         * specified joint.
         */
        fun rotationQuaternion(jp: JointPosition) : Quaternion {
            val q = identity()
            val rom = Rom()
            rom.setRoll(jp.orientation[0]*Math.PI/180.0)
            rom.setPitch(jp.orientation[1]*Math.PI/180.0)
            rom.setYaw(jp.orientation[2]*Math.PI/180.0)
            val rotation = multiply(multiply(rom.roll,rom.pitch),rom.yaw)
            q.insertRotation(rotation)
            /*
            for(col in 0..2) {
                for( row in 0..2 ) {
                    q.matrix[row][col] = rotation[row][col]
                }
            }
    */
            return q
        }
        /**
         * @return a quaternion for a simple rotation around the
         * root joint of a link, taking into account the current joint angle.
         */
        fun rotationQuaternion(jlink: JointLink) : Quaternion {
            val q = identity()
            val rom = Rom()
            rom.setRoll(jlink.orientation[0]*Math.PI/180.0)
            rom.setPitch((jlink.orientation[1]+jlink.theta)*Math.PI/180.0)
            rom.setYaw(jlink.orientation[2]*Math.PI/180.0)
            val rotation = multiply(multiply(rom.roll,rom.pitch),rom.yaw)
            q.insertRotation(rotation)
            /*
            for(col in 0..2) {
                for( row in 0..2 ) {
                    q.matrix[row][col] = rotation[row][col]
                }
            }
    */
            return q
        }
        /**
         * @return a quaternion for a translation from the source
         * joint to the end joint.
         */
        fun translationQuaternion(jlink: JointLink) : Quaternion {
            val q = identity()
            q.insertTranslation(jlink.coordinates)
            return q
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

        private val CLSS = "Quaternion"
    }


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
    }
}