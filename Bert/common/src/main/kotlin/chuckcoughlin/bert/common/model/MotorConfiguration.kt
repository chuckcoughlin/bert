/**
 * Copyright 2018-2019. Charles Coughlin. All Rights Reserved.
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
 * at runtime.
 */
class MotorConfiguration : Serializable {
    var joint: Joint
    var limb: Limb? = null
    var type = DynamixelType.MX28
        private set
    var id: Int
    var controller: String

    // Setting torque enable is essentially powering the motor on/off
    var isTorqueEnabled // Torque-enable - on/off 
            = false
    var offset // Configured position correction
            = 0.0
    var minAngle = 0.0
    var maxAngle = 0.0
    var maxSpeed = 0.0
        set(val) {
            field = `val`
        }
    var maxTorque = 0.0
        set(val) {
            field = `val`
        }
    var isDirect: Boolean

    // Save the current goal (or actual) values. All other members
    private var position // ~ degrees
            = 0.0
    var speed // ~ degrees/second
            = 0.0
    private var temperature // deg C
            = 0.0
    var torque // ~ N-m
            = 0.0
    var travelTime // ~msecs
            : Long = 0
        private set

    /**
     * Default constructor, necessary for serialization. Initialize all members.
     */
    constructor() {
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

    /**
     * Constructor: Sets configuration attributes. The rest are left at default jointValues.
     * @param j name of the motor (its joint)
     * @param type Dynamixel model
     * @param isDirect true if orientation is "forward"
     */
    constructor(j: Joint, type: DynamixelType, id: Int, cntrl: String, isDirect: Boolean) : super() {
        joint = j
        this.type = type
        this.id = id
        controller = cntrl
        this.isDirect = isDirect
    }

    // These are the current values
    fun getPosition(): Double {
        return position
    }

    fun setTemperature(temp: Double) {
        temperature = temp
    }

    fun setType(typ: String) {
        type = DynamixelType.valueOf(typ.uppercase(Locale.getDefault()))
    }

    fun setProperty(propertyName: String, value: Double) {
        try {
            val jp = JointProperty.valueOf(propertyName.uppercase(Locale.getDefault()))
            setProperty(jp, value)
        } catch (iae: IllegalArgumentException) {
            LOGGER.warning(
                String.format(
                    "%s.setProperty: Illegal property %s (%s)",
                    CLSS,
                    propertyName,
                    iae.localizedMessage
                )
            )
        }
    }

    /**
     * Use the motor value to set the corresponding property.
     * @param jp JointProperty that we are setting
     * @param value the new value
     */
    fun setProperty(jp: JointProperty?, value: Double) {
        when (jp) {
            JointProperty.POSITION -> setPosition(value)
            JointProperty.SPEED -> speed = value
            JointProperty.STATE -> setState(value)
            JointProperty.TEMPERATURE -> setTemperature(value)
            JointProperty.TORQUE -> torque = value
            else -> {}
        }
    }

    /**
     * When we set a new position, use the previous position and speed
     * to estimate the travel time.
     * @param p
     */
    fun setPosition(p: Double) {
        var delta = position - p
        if (delta < 0.0) delta = -delta
        if (speed > 0.0) travelTime = (1000.0 * delta / speed).toLong() // ~msecs
        position = p
    }

    fun setState(s: Double) {
        val state = s.toInt()
        isTorqueEnabled = state != 0
    }

    companion object {
        private const val CLSS = "MotorConfiguration"
        private val LOGGER = Logger.getLogger(CLSS)
        private const val serialVersionUID = -3452548869138158183L
    }
}