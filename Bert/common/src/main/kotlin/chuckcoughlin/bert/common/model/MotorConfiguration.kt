/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import java.io.Serializable
import java.util.*
import java.util.logging.Logger

/**
 * A motor refers to a Dynamixel stepper motor at one of the joints
 * of the robot. Each motor is in a group belonging to a single controller.
 * Optionally it may be part of a "Limb", a group of related motors.
 * All motors in a limb are managed by the same controller.
 *
 * The parameters here are configured in EEPROM. Some may be modifiable
 * at runtime, others are read-only
 */
class MotorConfiguration : Serializable {
    var joint: Joint
        private set
    var limb: Limb
    var type : DynamixelType
        private set
    var id: Int
        private set
    var controller: String
        private set
    // Setting torque enable is essentially powering the motor on/off
    var isTorqueEnabled : Boolean// Torque-enable - on/off
    var offset : Double // Configured position correction
    var minAngle : Double
    var maxAngle : Double
    var maxSpeed : Double
    var maxTorque : Double
    var isDirect: Boolean
        private set

    /**
     * When we set a new position, use the previous position and speed
     * to estimate the travel time.
     */
    var position : Double // ~ degrees
        get() = field
        set(value) {
            var delta = field - value
            if (delta < 0.0) delta = -delta
            if (speed > 0.0) travelTime = (1000.0 * delta / speed).toLong() // ~msecs
            field = value
    }
    var speed : Double // ~ degrees/second
    var temperature : Double // deg C
    var torque : Double // ~ N-m
    var travelTime : Long // ~msecs
        private set       // Read-only

    /**
     * Constructor: Sets configuration attributes. The rest are left at default jointValues.
     * @param j name of the motor (its joint)
     * @param type Dynamixel model
     * @param motorId address
     * @param name controller name
     * @param isDirect true if orientation is "forward"
     */
    constructor(j: Joint, type: DynamixelType, motorId: Int, name: String, isDirect: Boolean)  {
        id = motorId
        controller = name
        joint = j
        this.type = type
        this.isDirect = isDirect
    }



    fun setProperty(propertyName: String, value: Double) {
        try {
            val jp = JointProperty.valueOf(propertyName.uppercase(Locale.getDefault()))
            setProperty(jp, value)
        }
        catch (iae: IllegalArgumentException) {
            LOGGER.warning(
                String.format("%s.setProperty: Undefined property %s (%s)",
                    CLSS,propertyName,iae.localizedMessage
                )
            )
        }
    }

    /**
     * Use the motor value to set the corresponding property.
     * NOTE: In Kotlin, using the dot notation for a member
     *       actually accesses the custom getter or setter.
     */
    fun setProperty(jp: JointProperty, value: Double) {
        when (jp) {
            JointProperty.POSITION -> position = value
            JointProperty.SPEED -> speed = value
            JointProperty.STATE -> setState(value)
            JointProperty.TEMPERATURE -> temperature = value
            JointProperty.TORQUE -> torque = value
            else -> {
                val warning = String.format("%s.setProperty: Readonly property %s",
                        CLSS,jp.name)
                throw IllegalArgumentException(warning)
            }
        }
    }

    fun setState(s: Double) {
        val state = s.toInt()
        isTorqueEnabled = state != 0
    }

    fun setType(typ: String) {
        type = DynamixelType.valueOf(typ.uppercase(Locale.getDefault()))
    }


    companion object {
        private const val CLSS = "MotorConfiguration"
        private val LOGGER = Logger.getLogger(CLSS)
        private const val serialVersionUID = -3452548869138158183L
    }
    init {
        joint = Joint.UNKNOWN
        limb = Limb.UNKNOWN
        type = DynamixelType.MX28
        id = 0
        controller = ""
        offset = 0.0
        minAngle = -90.0
        maxAngle = 90.0
        maxSpeed = 600.0
        maxTorque = 1.9
        isDirect = true
        travelTime = 0
        // These are current goal settings
        position = 0.0 // Pure guess
        speed = 684.0 // Power-off AX-12
        temperature = 20.0 // Room temperature
        torque = 0.0 // Power-off value
        isTorqueEnabled = true // Initially torque is enabled
    }
}