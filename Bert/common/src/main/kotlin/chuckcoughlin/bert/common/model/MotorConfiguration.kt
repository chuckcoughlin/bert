/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import java.io.Serializable
import java.util.logging.Logger

/**
 * A motor refers to a Dynamixel stepper motor at one of the joints
 * of the robot. Each motor is in a group belonging to a single controller.
 * Optionally it may be part of a "Limb", a group of related motors.
 * All motors in a limb are managed by the same controller.
 *
 * The parameters here are configured in EEPROM. Some may be modifiable
 * at runtime, others are read-only
 * @param j name of the motor (its joint)
 * @param motorType Dynamixel model
 * @param motorId address
 * @param cname controller name
 * @param direct true if orientation is "forward"
 */
class MotorConfiguration(j: Joint, motorType: DynamixelType, motorId: Int, cname: String, direct: Boolean): Serializable {
    val controller: String
    val joint: Joint
    val type : DynamixelType
    val id: Int

    // Setting torque enable is essentially powering the motor on/off
    var isTorqueEnabled : Boolean// Torque-enable - on/off
    var limb: Limb
    var load: Double
    var minAngle : Double
    var maxAngle : Double
    var maxSpeed : Double
    var maxTorque : Double
    var offset : Double // Configured position correction
    var isDirect: Boolean
    var dispatchTime: Long  // Most recent timestamp on message containing this joint
    // When we set a new position, use the previous position and speed
    // to estimate the travel time.
    var angle = 0.0        // ~ degrees
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
    var voltage: Double

    /**
     * Set a motor property given a property enumeration and a value.
     * Attempts to set a read-only value fail silently.
     */
    fun setDynamicProperty(jp: JointDynamicProperty, value: Double) {
        when (jp) {
            JointDynamicProperty.ANGLE -> angle = value
            JointDynamicProperty.LOAD -> load = value
            JointDynamicProperty.MAXIMUMANGLE -> {}
            JointDynamicProperty.MINIMUMANGLE -> {}
            JointDynamicProperty.SPEED -> speed = value
            JointDynamicProperty.MAXIMUMSPEED -> {}
            JointDynamicProperty.STATE -> setState(value)
            JointDynamicProperty.TEMPERATURE -> temperature = value
            JointDynamicProperty.TORQUE -> torque = value
            JointDynamicProperty.MAXIMUMTORQUE -> {}
            JointDynamicProperty.VOLTAGE -> voltage = value
            JointDynamicProperty.RANGE -> {}  // Error - max and min must be set separately
            JointDynamicProperty.NONE-> {}
        }
    }

    fun setState(s: Double) {
        val state = s.toInt()
        isTorqueEnabled = state != 0
    }

    private val CLSS = "MotorConfiguration"
    private val LOGGER = Logger.getLogger(CLSS)

    /**
     * Set initial values for both definition and runtime parameters.
     */
    init {
        // Read-only
        controller = cname
        id = motorId
        isDirect = direct
        joint = j
        type = motorType

        limb = Limb.NONE
        load = 0.0
        offset = 0.0
        minAngle = -180.0
        maxAngle = 180.0
        maxSpeed = 600.0
        maxTorque = 1.9
        // Long enough ago to not delay first command
        dispatchTime = System.currentTimeMillis() - ConfigurationConstants.LONG_TME_AGO
        travelTime = 0
        // These are current goal settings
        speed = 684.0 // Power-off AX-12
        temperature = 20.0 // Room temperature
        torque = 0.0       // Power-off value
        isTorqueEnabled = true // Initially torque is enabled
        voltage = 0.0
    }
}