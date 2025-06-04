/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import chuckcoughlin.bert.common.math.Quaternion
import chuckcoughlin.bert.common.model.URDFModel.origin
import java.util.logging.Logger

/**
 * Internal Measurement Unit. This is the origin
 * of all chains. As for positioning, we allow
 * only rotations. The origin is fixed at (0,0,0)
 */
object IMU {
    val name:String

    val quaternion = Quaternion()
        get() = field

    // Do not allow origin to be altered externally
    private fun setCoordinates(x:Double,y:Double,z:Double) {
        if(DEBUG) LOGGER.info(String.format("%s.setCoordinates: %2.2f,%2.2f,%2.2f",CLSS,x,y,z))
        quaternion.setTranslation(x,y,z)
        quaternion.update()
    }
    // ~ degrees
    fun setRoll(angle:Double) {
        quaternion.setRoll(angle*Math.PI/180.0)
    }
    // ~ degrees
    fun setPitch(angle:Double) {
        quaternion.setPitch(angle*Math.PI/180.0)
    }
    // ~ degrees
    fun setYaw(angle:Double) {
        quaternion.setYaw(angle*Math.PI/180.0)
    }

    // Roll, pitch, yaw are in degrees. Convert to radians.
    fun setRpy(roll:Double,pitch:Double,yaw:Double) {
        setRoll(roll)
        setPitch(pitch)
        setYaw(yaw)
        quaternion.update()
    }
    // Incorporate any rotations that may have been set externally
     fun update() {
         quaternion.update()
    }

    private val CLSS = "IMU"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    init {
        name = CLSS
        DEBUG= RobotModel.debug.contains(ConfigurationConstants.DEBUG_SOLVER)
    }
}