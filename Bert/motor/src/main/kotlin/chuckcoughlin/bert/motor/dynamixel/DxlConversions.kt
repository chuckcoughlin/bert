/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.motor.dynamixel

import chuckcoughlin.bert.common.model.DynamixelType
import chuckcoughlin.bert.common.model.JointProperty
import chuckcoughlin.bert.common.model.MotorConfiguration
import java.util.*
import java.util.logging.Logger

/**
 * This class contains methods used to convert between Dynamixel jointValues and engineering units.
 * Code is derived from Pypot dynamixel.conversion.py
 * Protocol 1 only.
 */
object DxlConversions {
    fun degreeToDxl(mc: MotorConfiguration, arg: Double): Int {
        var value = arg
        if (value > mc.maxAngle) {
            LOGGER.warning(String.format("%s.degreeToDxl: %s attempted move to %.0f (max = %.0f)",
                CLSS, mc.joint.name, value, mc.maxAngle))
            value = mc.maxAngle
        }
        if (value < mc.minAngle) {
            LOGGER.warning(String.format("%s.degreeToDxl: %s attempted move to %.0f (min = %.0f)",
                CLSS, mc.joint.name, value, mc.minAngle))
            value = mc.minAngle
        }
        mc.position = value
        value = value - mc.offset
        val r = range[mc.type]!!
        if (!mc.isDirect) value = r - value
        val res = resolution[mc.type]!!
        var intVal = (value * res / r).toInt()
        intVal = intVal and res
        LOGGER.info(String.format("%s.degreeToDxl: %s b1,b2: %02X,%02X, offset %.0f %s",
                CLSS, mc.joint.name,(intVal shr 8).toByte(),intVal and 0xFF,mc.offset,
                if (mc.isDirect) "DIRECT" else "INDIRECT")
        )
        return intVal
    }

    // The range in degrees is split in increments by resolution. 180deg is "up" when looking at
    // the motor head-on. Zero is at the bottom. Zero degrees is not possible for an AX-12.
    // CW is < 180 deg. CCW > 180deg
    fun dxlToDegree(mc: MotorConfiguration, b1: Byte, b2: Byte): Double {
        var raw = (b1.toInt() and 0XFF) + 256 * (b2.toInt() and 0XFF)
        val res = resolution[mc.type]!!
        raw = raw and res
        val r = range[mc.type]!!
        var result = raw.toDouble() * r / res
        if (!mc.isDirect) result = r - result
        result = result + mc.offset
        //LOGGER.info(String.format("%s.dxlToDegree: %s b1,b2: %02X,%02X, offset %.0f %s result %.0f",CLSS,mc.getName().name(),b1,b2,
        //		mc.getOffset(),(mc.isDirect()?"DIRECT":"INDIRECT"),result));
        return result
    }

    // Speed is deg/sec
    fun speedToDxl(mc: MotorConfiguration, value: Double): Int {
        var cw: Boolean = mc.isDirect
        if (value < 0.0) cw = !cw
        var intVal = (value * 1023.0 / velocity[mc.type]!!).toInt()
        if (!cw) intVal = intVal or 0x400
        return intVal
    }

    fun dxlToSpeed(mc: MotorConfiguration, b1: Byte, b2: Byte): Double {
        var raw = (b1.toInt() and 0XFF) + 256 * (b2.toInt() and 0XFF)
        var cw: Boolean = mc.isDirect
        if (raw and 0x400 != 0) cw = !cw
        raw = raw and 0x3FF
        var result = (raw * velocity[mc.type]!!) / 1023
        if (!cw) result = -result
        return result
    }

    // N-m (assumes EEPROM max torque is 1023)
    // Positive number is CCW. Each unit represents .1%.
    fun torqueToDxl(mc: MotorConfiguration, value: Double): Int {
        var cw: Boolean = mc.isDirect
        if (value < 0.0) cw = !cw
        var intVal = (value * 1023.0 / torque[mc.type]!!).toInt()
        if (cw) intVal = intVal or 0x400
        return intVal

    }

    // For a load the direction is pertinent. Positive implies CW.
    fun dxlToLoad(mc: MotorConfiguration, b1: Byte, b2: Byte): Double {
        var raw = (b1.toInt() and 0XFF) + 256 * (b2.toInt() and 0XFF)
        var cw: Boolean = mc.isDirect
        if (raw and 0x400 != 0) cw = !cw
        raw = raw and 0x3FF
        var result = raw * torque[mc.type]!! / 1023.0
        if (cw) result = -result
        return result
    }

    // The torque-limit values in the EEPROM don't make sense. AX-12 has 8C FF. M-28/64 A0 FF.
    // For these values we'll just return the spec limit. Limit/goals are always positive.
    fun dxlToTorque(mc: MotorConfiguration, b1: Byte, b2: Byte): Double {
        var result = torque[mc.type]!! // Spec value
        if (b2.toInt() != 0xFF) {
            var raw = b1 + 256 * b2
            raw = raw and 0x3FF
            result = raw * result / 0x3FF
        }
        return result
    }

    // This is really a boolean. Take 0.0 to be false, 1.0 to be true
    fun dxlToTorqueEnable(mc: MotorConfiguration, b1: Byte): Double {
        var result = 1.0
        if (b1.toInt() == 0x0) result = 0.0
        return result
    }

    // deg C
    fun dxlToTemperature(value: Byte): Double {
        return value.toDouble()
    }

    // volts
    fun dxlToVoltage(value: Byte): Double {
        return value.toDouble() / 10.0
    }

    // Convert the named property to a control table address for the present state
    // of that property. These need to  be independent of motor type.
    fun addressForGoalProperty(name: String): Byte {
        var address: Byte = 0
        if (name.equals(JointProperty.POSITION.name, ignoreCase = true)) {
            address = GOAL_POSITION
        }
        else if (name.equals(JointProperty.SPEED.name, ignoreCase = true)) {
            address = GOAL_SPEED
        }
        else if (name.equals(JointProperty.TORQUE.name, ignoreCase = true)) {
            address = GOAL_TORQUE
        }
        else if (name.equals(JointProperty.STATE.name, ignoreCase = true)) {
            address = GOAL_TORQUE_ENABLE
        }
        else {
            LOGGER.warning(String.format("%s.addressForProperty: Unrecognized property name (%s)", CLSS, name))
        }
        return address
    }

    // Convert the named property to a control table address for the present state
    // of that property. These need to  be independent of motor type.
    fun addressForPresentProperty(name: String): Byte {
        var address: Byte = 0
        if (name.equals(JointProperty.MAXIMUMANGLE.name, ignoreCase = true)) {
            address = MAXIMUM_ANGLE
        }
        else if (name.equals(JointProperty.MINIMUMANGLE.name, ignoreCase = true)) {
            address = MINIMUM_ANGLE
        }
        else if (name.equals(JointProperty.POSITION.name, ignoreCase = true)) {
            address = PRESENT_POSITION
        }
        else if (name.equals(JointProperty.SPEED.name, ignoreCase = true)) {
            address = PRESENT_SPEED
        }
        else if (name.equals(JointProperty.TEMPERATURE.name, ignoreCase = true)) {
            address = PRESENT_TEMPERATURE
        }
        else if (name.equals(JointProperty.TORQUE.name, ignoreCase = true)) {
            address = PRESENT_LOAD
        }
        else if (name.equals(JointProperty.STATE.name, ignoreCase = true)) {
            address = GOAL_TORQUE_ENABLE
        }
        else if (name.equals(JointProperty.VOLTAGE.name, ignoreCase = true)) {
            address = PRESENT_VOLTAGE
        }
        else {
            LOGGER.warning(String.format("%s.addressForProperty: Unrecognized property name (%s)", CLSS, name))
        }
        return address
    }

    // Return the data length for the named property in the control table. We assume that
    // present values and goals are the same number of bytes.
    // Valid for Protocol 1 only.
    fun dataBytesForProperty(name: String): Byte {
        var length: Byte = 0
        if (name.equals(JointProperty.MAXIMUMANGLE.name, ignoreCase = true)) length = 2
        else if (name.equals(JointProperty.MINIMUMANGLE.name,ignoreCase = true)) {
            length = 2
        }
        else if (name.equals(JointProperty.POSITION.name, ignoreCase = true)) {
            length = 2
        }
        else if (name.equals(JointProperty.SPEED.name, ignoreCase = true)) {
            length = 2
        }
        else if (name.equals(JointProperty.TEMPERATURE.name,ignoreCase = true )) {
            length = 1
        }
        else if (name.equals(JointProperty.TORQUE.name, ignoreCase = true)) {
            length = 2
        }
        else if (name.equals(JointProperty.STATE.name, ignoreCase = true)) {
            length = 1
        }
        else if (name.equals(JointProperty.VOLTAGE.name,ignoreCase = true)) {
            length = 1
        }
        else {
            LOGGER.warning(String.format("%s.dataBytesForProperty: Unrecognized property name (%s)", CLSS, name))
        }
        return length
    }

    // Convert the value into a raw setting for the motor. Position is in degrees, speed and torque are percent.
    // Valid for Protocol 1 only.
    fun dxlValueForProperty(name: String, mc: MotorConfiguration, arg: Double): Int {
        var value = arg
        var dxlValue = 0
        if (name.equals(JointProperty.POSITION.name, ignoreCase = true)) dxlValue =
            degreeToDxl(mc, value)
        else if (name.equals(JointProperty.SPEED.name, ignoreCase = true)) {
            value = value * mc.maxSpeed / 100.0
            dxlValue = speedToDxl(mc, value)
        }
        else if (name.equals(JointProperty.TORQUE.name, ignoreCase = true)) {
            value = value * mc.maxTorque / 100.0
            dxlValue = torqueToDxl(mc, value)
        }
        else if (name.equals(JointProperty.STATE.name, ignoreCase = true)) {
            dxlValue = 1
            if (value == 0.0) dxlValue = 0
        }
        return dxlValue
    }

    // Convert the raw data bytes text describing the value and units. It may or may not use the second byte.
    // Presumably the ultimate will have more context.
    fun textForProperty(name: String, mc: MotorConfiguration, b1: Byte, b2: Byte): String {
        var name = name
        name = name.lowercase(Locale.getDefault())
        var text = ""
        val value = valueForProperty(name, mc, b1, b2)
        if (name.equals(JointProperty.MAXIMUMANGLE.name, ignoreCase = true)) {
            text = String.format("%.0f degrees", value)
        }
        else if (name.equals(JointProperty.MINIMUMANGLE.name,ignoreCase = true)) {
            text = String.format("%.0f degrees", value)
        }
        else if (name.equals(JointProperty.POSITION.name,ignoreCase = true)) {
            text = String.format("%.0f degrees", value)
        }
        else if (name.equals(JointProperty.SPEED.name,ignoreCase = true )) {
            text = String.format("%.0f degrees per second", value)
        }
        else if (name.equals(JointProperty.TEMPERATURE.name,ignoreCase = true )) {
            text = String.format("%.0f degrees centigrade", value)
        }
        else if (name.equals(JointProperty.TORQUE.name,ignoreCase = true)) {
            text = String.format("%.1f newton-meters", value)
        }
        else if (name.equals(JointProperty.STATE.name,ignoreCase = true)) {
            text = String.format("torque-%s", if (value == 0.0) "disabled" else "enabled")
        }
        else if (name.equals(JointProperty.VOLTAGE.name, ignoreCase = true)) {
            text = String.format("%.1f volts", value)
        }
        else {
            LOGGER.warning(String.format("%s.textForProperty: Unrecognized property name (%s)", CLSS, name))
        }
        return text
    }

    // Convert the raw data bytes into a double value. It may or may not use the second byte.
    // Value is engineering units.
    // Valid for Protocol 1 only.
    fun valueForProperty(name: String, mc: MotorConfiguration, b1: Byte, b2: Byte): Double {
        var value = 0.0
        if (name.equals(JointProperty.MAXIMUMANGLE.name, ignoreCase = true)) {
            value = dxlToDegree(mc, b1, b2)
        }
        else if (name.equals(JointProperty.MINIMUMANGLE.name, ignoreCase = true)) {
            value = dxlToDegree(mc, b1, b2)
        }
        else if (name.equals(JointProperty.POSITION.name, ignoreCase = true)) {
            value = dxlToDegree(mc, b1, b2)
        }
        else if (name.equals(JointProperty.SPEED.name, ignoreCase = true)) {
            value =
                dxlToSpeed(mc, b1, b2)
        }
        else if (name.equals(JointProperty.TEMPERATURE.name, ignoreCase = true)) {
            value = dxlToTemperature(b1)
        }
        else if (name.equals(JointProperty.TORQUE.name, ignoreCase = true)) {
            value = dxlToTorque(mc, b1, b2)
        }
        else if (name.equals(JointProperty.STATE.name, ignoreCase = true)) {
            value = dxlToTorqueEnable(mc, b1)
        }
        else if (name.equals(JointProperty.VOLTAGE.name, ignoreCase = true)) {
            value = dxlToVoltage(b1)
        }
        else {
            LOGGER.warning(String.format("%s.valueForProperty: Unrecognized property name (%s)", CLSS, name))
        }
        return value
    }

    // Object members ...
    const val CLSS = "DynamixelConversions"
    val LOGGER = Logger.getLogger(CLSS)
    val range = HashMap<DynamixelType, Int>()
    val resolution = HashMap<DynamixelType, Int>()
    val torque = HashMap<DynamixelType, Double>()
    val velocity = HashMap<DynamixelType, Double>()

    // Constants for control table addressses. These must be the same for MX28,MX64,AX12
    const val LIMIT_BLOCK_ADDRESS = 6.toByte()
    const val LIMIT_BLOCK_BYTES = 9.toByte()
    const val GOAL_BLOCK_ADDRESS = 0x1E.toByte()
    const val GOAL_BLOCK_BYTES = 6.toByte()
    const val GOAL_POSITION = 0x1E.toByte()
    const val GOAL_SPEED = 0x20.toByte()
    const val GOAL_TORQUE = 0x22.toByte()
    const val GOAL_TORQUE_ENABLE = 0x18.toByte()
    private const val MINIMUM_ANGLE = 0x06.toByte() // CCW
    private const val MAXIMUM_ANGLE = 0x08.toByte() // CW
    private const val PRESENT_LOAD = 0x28.toByte() // low, high bytes
    private const val PRESENT_POSITION = 0x24.toByte() // low, high bytes
    private const val PRESENT_SPEED = 0x26.toByte() // low, high bytes
    private const val PRESENT_TEMPERATURE = 0x2B.toByte() // single byte
    private const val PRESENT_VOLTAGE = 0x2A.toByte() // single byte

    init {
        // Range of motion in degrees
        range[DynamixelType.AX12] = 300
        range[DynamixelType.MX28] = 360
        range[DynamixelType.MX64] = 360

        // Increments for 360 deg by type
        resolution[DynamixelType.AX12] = 0x3FF
        resolution[DynamixelType.MX28] = 0xFFF
        resolution[DynamixelType.MX64] = 0xFFF

        // Angular velocity ~ deg/s
        velocity[DynamixelType.AX12] = 684.0
        velocity[DynamixelType.MX28] = 700.0
        velocity[DynamixelType.MX64] = 700.0

        // Full-scale torque ~ Nm
        torque[DynamixelType.AX12] = 1.2
        torque[DynamixelType.MX28] = 2.5
        torque[DynamixelType.MX64] = 6.0
    }
}
