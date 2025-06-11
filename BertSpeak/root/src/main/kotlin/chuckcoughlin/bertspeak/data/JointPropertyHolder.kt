/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.common.data

import chuckcoughlin.bertspeak.data.MotorData

/**
 * This class is a holder for numerical parameters
 * of a single joint.
 */
class JointPropertyHolder (val jnt:String,val angle:Double,val speed:Double,val load:Double,
                           val temperature:Double,val maxSpeed:Double,val maxTorque:Double) {
    fun toMotorData(): MotorData {
        val data = MotorData(this)
        return data
    }
}