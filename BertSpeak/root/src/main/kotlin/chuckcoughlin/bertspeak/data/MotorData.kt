/**
 * Copyright 2022-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.common.data.JointPropertyHolder
import java.util.Date

/**
 * Format the properties for display. Add timestamp.
 */
class MotorData(propertyHolder: JointPropertyHolder ) {
    val timestamp: Date
    val jnt:String
    val angle:String
    val speed:String
    val load:String
    val temperature:String
    val maxSpeed:String
    val maxTorque:String

    init {
        timestamp = Date()
        jnt = propertyHolder.jnt
        angle = String.format("%3.0f",propertyHolder.angle)
        load = String.format("%2.1f",propertyHolder.load)
        speed = String.format("%3.0f",propertyHolder.speed)
        temperature = String.format("%2.0f",propertyHolder.temperature)
        maxSpeed = String.format("%3.0f",propertyHolder.maxSpeed)
        maxTorque = String.format("%2.1f",propertyHolder.maxTorque)
    }
}
