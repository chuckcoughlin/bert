/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.motor.dynamixel

import chuckcoughlin.bert.common.BottleConstants
import chuckcoughlin.bert.common.model.DynamixelType
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointProperty
import chuckcoughlin.bert.common.model.MotorConfiguration
import java.util.logging.Logger

/**
 * This class contains utility methods used to create and interpret different varieties \
 * of Dynamixel serial messages. Code is derived from Pypot dynamixel.v2.py and the Dynamixel
 * documentation at http://emanual.robotis.com. Applies to MX64, MX28, AX12A models.
 * The documentation is unclear about the protocol version for AX-12 models, but it appears
 * to be Protocol 2.0. We have coded on this assumption.
 */
class DxlMessage {
    private val converter: DxlConversions

    /**
     * As each method that generates motor motions is invoked, it calculates time to execute the movement.
     * The result is stored as a static parameter, good only until the next method is run.
     * @return the maximum travel time as calculated by the most recent byte syntax generator. Time ~msecs.
     */
    var mostRecentTravelTime: Long = 0
        private set

    init {
        converter = DxlConversions()
    }

    /**
     * Iterate through the list of motor configurations to determine which, if any, are outside the max-min
     * angle ranges. For those outside, move the position to a legal value.
     * WARNING: SYNC_WRITE requests do not generate responses.
     * Discount any current readings of zero, it probably means that the motor positions were never evaluated.
     * @param configurations a list of motor configuration objects
     * @return list of byte arrays with bulk read plus extras for any AX-12.
     */
    fun byteArrayListToInitializePositions(configurationsByName: Map<String?, MotorConfiguration?>): List<ByteArray> {
        val outliers: MutableList<MotorConfiguration?> =
            ArrayList<MotorConfiguration?>() // Will hold the joints that need moving.
        mostRecentTravelTime = 0
        for (mc in configurationsByName.values) {
            val pos: Double = mc.getPosition()
            if (pos == 0.0) {
                LOGGER.info(
                    java.lang.String.format(
                        "%s.byteArrayListToInitializePositions: %s never evaluated, ignored",
                        CLSS,
                        mc.getJoint().name()
                    )
                )
            } else if (pos > mc.getMaxAngle()) {
                LOGGER.info(
                    java.lang.String.format(
                        "%s.byteArrayListToInitializePositions: %s out-of-range at %.0f (max=%.0f)",
                        CLSS, mc.getJoint().name(), pos, mc.getMaxAngle()
                    )
                )
                mc.setPosition(mc.getMaxAngle())
                outliers.add(mc)
                if (mc.getTravelTime() > mostRecentTravelTime) mostRecentTravelTime = mc.getTravelTime()
            } else if (pos < mc.getMinAngle()) {
                LOGGER.info(
                    java.lang.String.format(
                        "%s.byteArrayListToInitializePositions: %s out-of-range at %.0f (min=%.0f)",
                        CLSS, mc.getJoint().name(), pos, mc.getMinAngle()
                    )
                )
                mc.setPosition(mc.getMinAngle())
                outliers.add(mc)
                if (mc.getTravelTime() > mostRecentTravelTime) mostRecentTravelTime = mc.getTravelTime()
            }
        }
        // Add heuristics to avoid some common entanglements. Hip is only present in lower controller
        // No knock-knees
        var leftHip: MotorConfiguration? = configurationsByName[Joint.LEFT_HIP_X.name()]
        if (leftHip != null) {
            var rightHip: MotorConfiguration? = configurationsByName[Joint.RIGHT_HIP_X.name()]
            if (leftHip.getPosition() > MAX_HIP_X) {
                leftHip.setPosition(MAX_HIP_X)
                outliers.add(leftHip)
            }
            if (rightHip.getPosition() > MAX_HIP_X) {
                rightHip.setPosition(MAX_HIP_X)
                outliers.add(rightHip)
            }
            // No pidgin toes
            leftHip = configurationsByName[Joint.LEFT_HIP_X.name()]
            rightHip = configurationsByName[Joint.LEFT_HIP_X.name()]
            if (leftHip.getPosition() < MIN_HIP_Z) {
                leftHip.setPosition(MIN_HIP_Z)
                outliers.add(leftHip)
            }
            if (rightHip.getPosition() < MIN_HIP_Z) {
                rightHip.setPosition(MIN_HIP_Z)
                outliers.add(rightHip)
            }
        }
        val messages: MutableList<ByteArray> = ArrayList()
        val pc = outliers.size
        // Positions
        if (pc > 0) {
            val len = 3 * pc + 8 //  3 bytes per motor + address + byte count + header + checksum
            val bytes = ByteArray(len)
            setSyncWriteHeader(bytes)
            bytes[3] = (len - 4).toByte()
            bytes[4] = SYNC_WRITE
            bytes[5] = converter.addressForGoalProperty(JointProperty.POSITION.name())
            bytes[6] = 0x2 // 2 bytes
            var index = 7
            for (mc in outliers) {
                LOGGER.info(
                    java.lang.String.format(
                        "%s.byteArrayListToInitializePositions: set position for %s to %.0f",
                        CLSS,
                        mc.getJoint().name(),
                        mc.getPosition()
                    )
                )
                val dxlValue = converter.dxlValueForProperty(JointProperty.POSITION.name(), mc, mc.getPosition())
                bytes[index] = mc.getId() as Byte
                bytes[index + 1] = (dxlValue and 0xFF).toByte()
                bytes[index + 2] = (dxlValue shr 8).toByte()
                index = index + 3
            }
            setChecksum(bytes)
            messages.add(bytes)
        }
        return messages
    }

    /**
     * Create a bulk read message to interrogate a list of motors for a specified
     * property. Unfortunately AX-12 motors do not support this request, so must be
     * queried separately (thus the list). Note that the bulk read results in individual
     * responses from each motor.
     * @param propertyName the name of the desired property (must be a joint property)
     * @return list of byte arrays with bulk read plus extras for any AX-12.
     */
    fun byteArrayListToListProperty(
        propertyName: String,
        configurations: Collection<MotorConfiguration?>
    ): List<ByteArray> {
        val messages: MutableList<ByteArray> = ArrayList()
        var count = configurations.size // Number of motors, less AX-12
        for (mc in configurations) {
            if (mc.getType().equals(DynamixelType.AX12)) {
                val length = 4 // Remaining bytes past length including checksum
                val bytes = ByteArray(length + 4) // Account for header and length
                setHeader(bytes, mc.getId())
                bytes[3] = length.toByte()
                bytes[4] = READ
                bytes[5] = converter.addressForPresentProperty(propertyName)
                bytes[6] = converter.dataBytesForProperty(propertyName)
                setChecksum(bytes)
                messages.add(bytes)
                count--
            }
        }

        // Now lay out the bulk read message for everyone else.
        val length = 3 * count + 3 // Remaining bytes past length including checksum
        val bytes = ByteArray(length + 4) // Account for header and length
        setHeader(bytes, BROADCAST_ID.toInt())
        bytes[3] = length.toByte()
        bytes[4] = BULK_READ
        bytes[5] = 0
        var addr = 6
        for (mc in configurations) {
            if (mc.getType().equals(DynamixelType.AX12)) continue
            bytes[addr] = converter.dataBytesForProperty(propertyName)
            bytes[addr + 1] = mc.getId() as Byte
            bytes[addr + 2] = converter.addressForPresentProperty(propertyName)
            addr += 3
        }
        setChecksum(bytes)
        messages.add(bytes)
        return messages
    }

    /**
     * Set either the speed, torque or torque_enable for all motors in the configuration map.
     * WARNING: SYNC_WRITE requests do not generate responses.
     * @param map of the motor configurations keyed by joint name
     * @param property, either speed,torque or torque_enable
     * @return a byte array with entries corresponding to joints, if any.
     */
    fun byteArrayToSetProperty(map: Map<String?, MotorConfiguration?>, property: String): ByteArray? {
        // First count all the joints in the limb
        val count = map.size
        var bytes: ByteArray? = null
        var dxlValue = 0
        if (count > 0) {
            val len =
                (converter.dataBytesForProperty(property) + 1) * count + 8 //  2 or 3 bytes per motor + address + byte count + header + checksum
            bytes = ByteArray(len)
            setSyncWriteHeader(bytes)
            bytes[3] = (len - 4).toByte()
            bytes[4] = SYNC_WRITE
            bytes[5] = converter.addressForGoalProperty(property)
            bytes[6] = converter.dataBytesForProperty(property)
            var index = 7
            for (mc in map.values) {
                bytes[index] = mc.getId() as Byte
                if (property.equals(JointProperty.TORQUE.name(), ignoreCase = true)) {
                    dxlValue = converter.dxlValueForProperty(JointProperty.TORQUE.name(), mc, mc.getTorque())
                    bytes[index + 1] = (dxlValue and 0xFF).toByte()
                    bytes[index + 2] = (dxlValue shr 8).toByte()
                } else if (property.equals(JointProperty.SPEED.name(), ignoreCase = true)) {
                    dxlValue = converter.dxlValueForProperty(JointProperty.SPEED.name(), mc, mc.getSpeed())
                    bytes[index + 1] = (dxlValue and 0xFF).toByte()
                    bytes[index + 2] = (dxlValue shr 8).toByte()
                } else if (property.equals(JointProperty.STATE.name(), ignoreCase = true)) {
                    dxlValue = converter.dxlValueForProperty(
                        JointProperty.STATE.name(),
                        mc,
                        if (mc.isTorqueEnabled()) 1.0 else 0.0
                    )
                    bytes[index + 1] = dxlValue.toByte()
                }
                index = index + 1 + converter.dataBytesForProperty(property)
            }
            setChecksum(bytes)
        }
        return bytes
    }

    /**
     * A pose may consist of any or all of position, speed and torque for the motors it refrerences. Query the database
     * to get values. Skip any that have null values. There is a hardware limit of 143 bytes for each array (shouldn't be a problem).
     * WARNING: SYNC_WRITE requests, apparently, do not generate responses.
     * @param map of the motor configurations keyed by joint name
     * @param pose name of the pose to be set
     * @return up to 3 byte arrays as required by the pose
     */
    fun byteArrayListToSetPose(map: Map<String?, MotorConfiguration?>, pose: String?): List<ByteArray> {
        val db: Database = Database.getInstance()
        val torques: Map<String?, Double> = db.getPoseJointValuesForParameter(map, pose, "torque")
        val speeds: Map<String?, Double> = db.getPoseJointValuesForParameter(map, pose, "speed")
        val positions: Map<String?, Double> = db.getPoseJointValuesForParameter(map, pose, "position")
        val messages: MutableList<ByteArray> = ArrayList()
        // First set torques, then speeds, then positions
        val tc = torques.size
        // Torque
        if (tc > 0) {
            val len = 3 * tc + 8 //  3 bytes per motor + address + byte count + header + checksum
            val bytes = ByteArray(len)
            setSyncWriteHeader(bytes)
            bytes[3] = (len - 4).toByte()
            bytes[4] = SYNC_WRITE
            bytes[5] = converter.addressForGoalProperty(JointProperty.TORQUE.name())
            bytes[6] = 0x2 // 2 bytes
            var index = 7
            for (key in torques.keys) {
                val mc: MotorConfiguration? = map[key]
                val dxlValue = converter.dxlValueForProperty(JointProperty.TORQUE.name(), mc, torques[key]!!)
                bytes[index] = mc.getId() as Byte
                bytes[index + 1] = (dxlValue and 0xFF).toByte()
                bytes[index + 2] = (dxlValue shr 8).toByte()
                mc.setTorque(torques[key]) // percent of max
                index = index + 3
            }
            setChecksum(bytes)
            messages.add(bytes)
        }
        val sc = speeds.size
        // Speed
        if (sc > 0) {
            val len = 3 * sc + 8 //  3 bytes per motor + address + byte count + header + checksum
            val bytes = ByteArray(len)
            setSyncWriteHeader(bytes)
            bytes[3] = (len - 4).toByte()
            bytes[4] = SYNC_WRITE
            bytes[5] = converter.addressForGoalProperty(JointProperty.SPEED.name())
            bytes[6] = 0x2 // 2 bytes
            var index = 7
            for (key in speeds.keys) {
                val mc: MotorConfiguration? = map[key]
                val dxlValue = converter.dxlValueForProperty(JointProperty.SPEED.name(), mc, speeds[key]!!)
                bytes[index] = mc.getId() as Byte
                bytes[index + 1] = (dxlValue and 0xFF).toByte()
                bytes[index + 2] = (dxlValue shr 8).toByte()
                mc.setSpeed(speeds[key]) // percent of max
                index = index + 3
            }
            setChecksum(bytes)
            messages.add(bytes)
        }
        val pc = positions.size
        // Positions
        if (pc > 0) {
            mostRecentTravelTime = 0
            val len = 3 * pc + 8 //  3 bytes per motor + address + byte count + header + checksum
            val bytes = ByteArray(len)
            setSyncWriteHeader(bytes)
            bytes[3] = (len - 4).toByte()
            bytes[4] = SYNC_WRITE
            bytes[5] = converter.addressForGoalProperty(JointProperty.POSITION.name())
            bytes[6] = 0x2 // 2 bytes
            var index = 7
            for (key in positions.keys) {
                val mc: MotorConfiguration? = map[key]
                //LOGGER.info(String.format("%s.bytesToSetPose: Id = %d - set position for %s to %.0f",CLSS,mc.getId(),key,positions.get(key)));
                val dxlValue = converter.dxlValueForProperty(JointProperty.POSITION.name(), mc, positions[key]!!)
                bytes[index] = mc.getId() as Byte
                bytes[index + 1] = (dxlValue and 0xFF).toByte()
                bytes[index + 2] = (dxlValue shr 8).toByte()
                mc.setPosition(positions[key])
                if (mc.getTravelTime() > mostRecentTravelTime) mostRecentTravelTime = mc.getTravelTime()
                index = index + 3
            }
            setChecksum(bytes)
            messages.add(bytes)
        }
        return messages
    }

    /**
     * Create a serial message to broadcast a ping request to all motors.
     * This is taken directly from http://emanual.robotis.com/docs/en/dxl/protocol1/
     * @return byte array for message
     */
    fun bytesToBroadcastPing(): ByteArray {
        val length = 2 // Remaining bytes past length including checksum
        val bytes = ByteArray(length + 4) // Account for header and length
        setHeader(bytes, BROADCAST_ID.toInt())
        bytes[3] = length.toByte()
        bytes[4] = PING
        setChecksum(bytes)
        return bytes
    }

    /**
     * Create a serial message to read the current goals of a particular motor.
     * @param id of the motor
     * @return byte array with command to read the block of RAM
     */
    fun bytesToGetGoals(id: Int): ByteArray {
        val length = 4 // Remaining bytes past length including checksum
        val bytes = ByteArray(length + 4) // Account for header and length
        setHeader(bytes, id)
        bytes[3] = length.toByte()
        bytes[4] = READ
        bytes[5] = DxlConversions.GOAL_BLOCK_ADDRESS
        bytes[6] = DxlConversions.GOAL_BLOCK_BYTES
        setChecksum(bytes)
        return bytes
    }

    /**
     * Create a serial message to read the block of limits contained in EEPROM
     * for a particular motor.
     * @param id of the motor
     * @return byte array with command to read the block of EEPROM
     */
    fun bytesToGetLimits(id: Int): ByteArray {
        val length = 4 // Remaining bytes past length including checksum
        val bytes = ByteArray(length + 4) // Account for header and length
        setHeader(bytes, id)
        bytes[3] = length.toByte()
        bytes[4] = READ
        bytes[5] = DxlConversions.LIMIT_BLOCK_ADDRESS
        bytes[6] = DxlConversions.LIMIT_BLOCK_BYTES
        setChecksum(bytes)
        return bytes
    }

    /**
     * Create a serial message to read a specified property of a motor.
     * @param id of the motor
     * @param propertyName the name of the desired property (must be a joint property)
     * @return byte array with command to read the property
     */
    fun bytesToGetProperty(id: Int, propertyName: String): ByteArray {
        val length = 4 // Remaining bytes past length including checksum
        val bytes = ByteArray(length + 4) // Account for header and length
        setHeader(bytes, id)
        bytes[3] = length.toByte()
        bytes[4] = READ
        bytes[5] = converter.addressForPresentProperty(propertyName)
        bytes[6] = converter.dataBytesForProperty(propertyName)
        setChecksum(bytes)
        return bytes
    }

    /**
     * Create a serial message to write a goal for the motor. Recognized properties are:
     * position, speed, torque and torque_enable. All except torque enable are two byte parameters.
     * @param id of the motor
     * @param propertyName the name of the desired property (must be a joint property)
     * @return byte array with command to read the property
     */
    fun bytesToSetProperty(mc: MotorConfiguration, propertyName: String, value: Double): ByteArray {
        val dxlValue = converter.dxlValueForProperty(propertyName, mc, value)
        val length = 3 + converter.dataBytesForProperty(propertyName) // Remaining bytes past length including checksum
        val bytes = ByteArray(length + 4) // Account for header and length
        setHeader(bytes, mc.getId())
        bytes[3] = length.toByte()
        bytes[4] = WRITE
        bytes[5] = converter.addressForGoalProperty(propertyName)
        if (converter.dataBytesForProperty(propertyName).toInt() == 2) {
            bytes[6] = (dxlValue and 0xFF).toByte()
            bytes[7] = (dxlValue shr 8).toByte()
        } else {
            bytes[6] = dxlValue.toByte()
        }
        setChecksum(bytes)
        if (propertyName.equals(JointProperty.POSITION.name(), ignoreCase = true)) {
            mc.setPosition(value)
            mostRecentTravelTime = mc.getTravelTime()
        } else if (propertyName.equals(
                JointProperty.SPEED.name(),
                ignoreCase = true
            )
        ) mc.setSpeed(value) else if (propertyName.equals(
                JointProperty.STATE.name(),
                ignoreCase = true
            )
        ) mc.setTorqueEnabled(
            if (value == 0.0) false else true
        ) else if (propertyName.equals(
                JointProperty.TEMPERATURE.name(),
                ignoreCase = true
            )
        ) mc.setTemperature(value) else if (propertyName.equals(
                JointProperty.TORQUE.name(),
                ignoreCase = true
            )
        ) mc.setTorque(value)
        return bytes
    }

    /**
     * Create a string suitable for printing and debugging.
     * @param bytes
     * @return a formatted string of the bytes as hex digits.
     */
    fun dump(bytes: ByteArray?): String {
        val sb = StringBuffer()
        var index = 0
        if (bytes != null) {
            while (index < bytes.size) {
                //if( bytes[index]=='\0') break;
                sb.append(String.format("%02X", bytes[index]))
                sb.append(" ")
                index++
            }

            // Add the buffer length
            sb.append("(")
            sb.append(bytes.size)
            sb.append(")")
        } else {
            sb.append("null message")
        }
        return sb.toString()
    }

    /**
     * Scan the supplied byte array looking for the message start markers.
     * When found return the buffer less any leading junk.
     * @param bytes
     * @return buffer guaranteed to be a legal message start, else null.
     */
    fun ensureLegalStart(bytes: ByteArray): ByteArray? {
        var i = 0
        while (i < bytes.size - 2) {
            if (bytes[i] == 0xFF.toByte() &&
                bytes[i] == 0xFF.toByte()
            ) {
                return if (i == 0) bytes else {
                    val copy = ByteArray(bytes.size - i)
                    System.arraycopy(bytes, i, copy, 0, copy.size)
                    LOGGER.warning(
                        String.format(
                            "%s.ensureLegalStart: cut %d bytes to provide legal msg",
                            CLSS,
                            i
                        )
                    )
                    copy
                }
            }
            i++
        }
        return null
    }

    /**
     * The only interesting information in a status message from a write
     * to a single device is the error code.
     * @param bytes
     * @return
     */
    fun errorMessageFromStatus(bytes: ByteArray): String? {
        var msg: String? = null
        if (bytes.size > 4) {
            val error = bytes[4]
            if (error.toInt() != 0x00) {
                val id = bytes[2]
                msg = String.format("Motor %d encountered %s", id, descriptionForError(error))
                //LOGGER.severe(msg);
            }
        }
        return msg
    }

    /**
     * Extract the message length.
     * @param bytes
     * @return the total number of bytes in message, else -1 if there are too few bytes to tell.
     */
    fun getMessageLength(bytes: ByteArray): Int {
        var len = -1
        if (bytes.size > 4) {
            len = bytes[3] + 4
        }
        return len
    }

    /**
     * Analyze a response buffer returned from a request for goal values for a motor. Goals
     * parameters are: position, speed, torque. Results will be entered in the properties map.
     * Convert speeds and torques to percent of max disregarding direction.
     * @param type the model of the motor
     * @param isDirect the orientation of the motor
     * @param props properties from a MessageBottle
     * @param bytes status response from the controller
     */
    fun updateGoalsFromBytes(mc: MotorConfiguration, props: MutableMap<String?, String?>, bytes: ByteArray) {
        var msg = ""
        if (verifyHeader(bytes)) {
            msg = String.format("%s.updateGoalsFromBytes: %s", CLSS, dump(bytes))
            val id = bytes[2].toInt()
            val err = bytes[4]
            var parameterName: String = JointProperty.POSITION.name()
            val v1 = converter.valueForProperty(parameterName, mc, bytes[5], bytes[6])
            val t1 = converter.textForProperty(parameterName, mc, bytes[5], bytes[6])
            props[parameterName] = v1.toString()
            mc.setPosition(v1)
            parameterName = JointProperty.SPEED.name() // Non-directional
            var v2 = converter.valueForProperty(parameterName, mc, bytes[7], bytes[8])
            val t2 = converter.textForProperty(parameterName, mc, bytes[7], bytes[8])
            props[parameterName] = v2.toString()
            v2 = v2 * 100.0 / DxlConversions.velocity!!.get(mc.getType())!! // Convert to percent
            mc.setSpeed(v2)
            parameterName = JointProperty.TORQUE.name() // Non-directional
            var v3 = converter.valueForProperty(parameterName, mc, bytes[9], bytes[10])
            val t3 = converter.textForProperty(parameterName, mc, bytes[9], bytes[10])
            props[parameterName] = v3.toString()
            v3 = v2 * 100.0 / DxlConversions.torque!!.get(mc.getType())!! // Convert to percent
            mc.setTorque(v3)
            val text = String.format("Goal position, speed and torque are : %s, %s, %s", t1, t2, t3)
            if (err.toInt() == 0) {
                props[BottleConstants.TEXT] = text
            } else {
                msg = String.format(
                    "%s.updateGoalsFromBytes: message returned error %d (%s)",
                    CLSS,
                    err,
                    descriptionForError(err)
                )
                props[BottleConstants.ERROR] = msg
                LOGGER.severe(msg)
            }
        } else {
            msg = String.format("%s.updateGoalsFromBytes: Illegal message: %s", CLSS, dump(bytes))
            props[BottleConstants.ERROR] = msg
            LOGGER.severe(msg)
        }
    }

    /**
     * Analyze a response buffer returned from a request for EEPROM limits for a motor. Limit
     * parameters are: angles, temperature, voltage and torque. Of these we extract only the
     * angles and torque. These are NOT corrected for offset or orientation.
     * @param type the model of the motor
     * @param isDirect the orientation of the motor
     * @param props properties from a MessageBottle
     * @param bytes status response from the controller
     */
    fun updateLimitsFromBytes(mc: MotorConfiguration, props: MutableMap<String?, String?>, bytes: ByteArray) {
        var msg = ""
        mc.setIsDirect(true)
        mc.setOffset(0.0)
        if (verifyHeader(bytes)) {
            msg = String.format("%s.updateLimitsFromBytes: %s", CLSS, dump(bytes))
            val id = bytes[2].toInt()
            val err = bytes[4]
            var parameterName: String = JointProperty.MAXIMUMANGLE.name() // CW
            val v1 = converter.valueForProperty(parameterName, mc, bytes[5], bytes[6])
            val t1 = converter.textForProperty(parameterName, mc, bytes[5], bytes[6])
            props[parameterName] = v1.toString()
            parameterName = JointProperty.MINIMUMANGLE.name() // CCW
            val v2 = converter.valueForProperty(parameterName, mc, bytes[7], bytes[8])
            val t2 = converter.textForProperty(parameterName, mc, bytes[7], bytes[8])
            props[parameterName] = v2.toString()
            parameterName = JointProperty.TORQUE.name() // Non-directional
            val v3 = converter.valueForProperty(parameterName, mc, bytes[12], bytes[13])
            val t3 = converter.textForProperty(parameterName, mc, bytes[12], bytes[13])
            props[parameterName] = v3.toString()
            val text = String.format("min, max angle and torque limits are : %s, %s, %s", t2, t1, t3)
            if (err.toInt() == 0) {
                props[BottleConstants.TEXT] = text
            } else {
                msg = String.format(
                    "%s.updateLimitsFromBytes: message returned error %d (%s)",
                    CLSS,
                    err,
                    descriptionForError(err)
                )
                props[BottleConstants.ERROR] = msg
                LOGGER.severe(msg)
            }
        } else {
            msg = String.format("%s.updateLimitsFromBytes: Illegal message: %s", CLSS, dump(bytes))
            props[BottleConstants.ERROR] = msg
            LOGGER.severe(msg)
        }
    }

    /**
     * Analyze a response buffer for some parameter of a motor. Augment the
     * supplied Properties with the result (possibly an error).
     * @param parameterName the requested parameter
     * @param type the model of the motor
     * @param isDirect the orientation of the motor
     * @param props properties from a MessageBottle
     * @param bytes status response from the controller
     */
    fun updateParameterFromBytes(
        parameterName: String,
        mc: MotorConfiguration,
        props: MutableMap<String?, String?>,
        bytes: ByteArray
    ) {
        var msg = ""
        if (verifyHeader(bytes)) {
            msg = String.format("%s.updateParameterFromBytes: %s", CLSS, dump(bytes))
            val id = bytes[2].toInt()
            val err = bytes[4]
            val value = converter.valueForProperty(parameterName, mc, bytes[5], bytes[6])
            val text = converter.textForProperty(parameterName, mc, bytes[5], bytes[6])
            if (err.toInt() == 0) {
                props[BottleConstants.PROPERTY_NAME] = parameterName
                props[BottleConstants.TEXT] = text
                props[parameterName] = String.format("%.2f", value)
                mc.setProperty(parameterName, value)
                LOGGER.info(
                    java.lang.String.format(
                        "%s.updateParameterFromBytes: %s %s=%.2f",
                        CLSS,
                        mc.getJoint(),
                        parameterName,
                        value
                    )
                )
            } else {
                msg = String.format(
                    "%s.updateParameterFromBytes: message returned error %d (%s)",
                    CLSS,
                    err,
                    descriptionForError(err)
                )
                props[BottleConstants.ERROR] = msg
                LOGGER.severe(msg)
            }
        } else {
            msg = String.format("%s.updateParameterFromBytes: Illegal message: %s", CLSS, dump(bytes))
            props[BottleConstants.ERROR] = msg
            LOGGER.severe(msg)
        }
    }

    /**
     * Analyze the response buffer for the indicated motor parameter and
     * update the supplied position map accordingly. There may be several responses concatenated.
     * We assume that the parameter is 2 bytes. Log errors, there's not much else we can do.
     * @param parameterName the name of the Joint property being handled
     * @param configurations a map of motor configurations by id
     * @param bytes status response from the controller
     * @param parameters an array of positions by id, supplied. This is augmented by the method.
     */
    fun updateParameterArrayFromBytes(
        parameterName: String,
        configurations: Map<Int?, MotorConfiguration?>,
        bytes: ByteArray,
        parameters: MutableMap<Int?, String?>
    ) {
        var msg = ""
        var length = 7
        var index = 0
        while (index < bytes.size) {
            // LOGGER.info(String.format("%s.updateParameterArrayFromBytes: index %d of %d",CLSS,index,bytes.length));
            if (verifyHeader(bytes, index)) {
                val id = bytes[index + 2].toInt()
                length = bytes[index + 3] + 4 // Takes care of fixed bytes pre-length
                val err = bytes[index + 4]
                val mc: MotorConfiguration? = configurations[id]
                if (err.toInt() == 0 && mc != null && bytes.size > index + 6) {
                    val param = converter.valueForProperty(parameterName, mc, bytes[index + 5], bytes[index + 6])
                    parameters[id] = String.format("%.2f", param)
                    mc.setProperty(parameterName, param)
                    LOGGER.info(
                        java.lang.String.format(
                            "%s.updateParameterArrayFromBytes: %s %s=%.0f",
                            CLSS,
                            mc.getJoint(),
                            parameterName,
                            param
                        )
                    )
                } else if (err.toInt() != 0) {
                    msg = String.format(
                        "%s.updateParameterArrayFromBytes: motor %d returned error %d (%s)", CLSS, id, err,
                        descriptionForError(err)
                    )
                    LOGGER.severe(msg)
                } else if (mc == null) {
                    msg = String.format(
                        "%s.updateParameterArrayFromBytes: motor %d not supplied in motor configurations",
                        CLSS,
                        id,
                        dump(bytes)
                    )
                    LOGGER.severe(msg)
                } else if (bytes.size <= index + 6) {
                    msg = String.format(
                        "%s.updateParameterArrayFromBytes: motor %d input truncated (%s)",
                        CLSS,
                        id,
                        dump(bytes)
                    )
                    LOGGER.severe(msg)
                } else {
                    msg = String.format(
                        "%s.updateParameterArrayFromBytes: programming error at id=%d",
                        CLSS,
                        id,
                        dump(bytes)
                    )
                    LOGGER.severe(msg)
                }
            } else {
                LOGGER.severe(
                    String.format(
                        "%s.updateParameterArrayFromBytes: Header not found: %s",
                        CLSS,
                        dump(bytes)
                    )
                )
            }
            index = index + length
        }
    }

    // ===================================== Private Methods =====================================
    // Return a string describing the error. We only check one bit.
    private fun descriptionForError(err: Byte): String {
        var description = "Unrecognized error"
        if (err.toInt() and 0x01 != 0x00) description =
            "an instruction error" else if (err.toInt() and 0x02 != 0x00) description =
            "an overload error" else if (err.toInt() and 0x04 != 0x00) description =
            "an incorrect checksum" else if (err.toInt() and 0x08 != 0x00) description =
            "a range error" else if (err.toInt() and 0x10 != 0x00) description =
            "overheating" else if (err.toInt() and 0x20 != 0x00) description =
            "a position outside angle limits" else if (err.toInt() and 0x40 != 0x00) description =
            "an input voltage outside the acceptable range"
        return description
    }

    // Set the header up until the length field. The header includes the device ID.
    // Protocol 1. 3 bytes
    private fun setHeader(bytes: ByteArray, id: Int) {
        bytes[0] = 0xFF.toByte()
        bytes[1] = 0xFF.toByte()
        bytes[2] = id.toByte()
    }

    // Set the header up until the length field. The header includes the device ID.
    // Protocol 1. 3 bytes
    private fun setSyncWriteHeader(bytes: ByteArray) {
        bytes[0] = 0xFF.toByte()
        bytes[1] = 0xFF.toByte()
        bytes[2] = 0xFE.toByte()
    }

    /**
     * Consider bytes 0-(len-2), then insert into last bytes. "oversize" variables
     * to avoid problem with no "unsigned" in Java. Ultimately we discard all except
     * low order bits.
     * @see http://emanual.robotis.com/docs/en/dxl/protocol1/
     *
     * @param buf the byte buffer
     */
    fun setChecksum(buf: ByteArray) {
        val size = buf.size - 1 // Exclude bytes that hold Checksum
        var sum = 0 // Instruction checksum.
        for (j in 2 until size) {
            sum = sum + buf[j]
        }
        sum = sum and 0xFF
        buf[size] = (255 - sum).toByte()
    }

    /**
     * Protocol 1
     */
    private fun verifyHeader(bytes: ByteArray): Boolean {
        var result = false
        if (bytes.size > 5 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFF.toByte()) {
            result = true
        }
        return result
    }

    private fun verifyHeader(bytes: ByteArray, index: Int): Boolean {
        var result = false
        if (bytes.size > index + 5 && bytes[index] == 0xFF.toByte() && bytes[index + 1] == 0xFF.toByte()) {
            result = true
        }
        return result
    }

    companion object {
        private const val CLSS = "DxlMessage"
        private val LOGGER = Logger.getLogger(CLSS)

        // Constants for the instructions
        private const val BROADCAST_ID = 0xFE.toByte() // ID to transmit to all devices connected to port
        private const val PING: Byte = 0x01 // Instruction that checks whether the Packet has arrived
        private const val READ: Byte = 0x02 // Instruction to read data from the Device
        private const val WRITE: Byte = 0x03 // Instruction to write data on the Device
        private const val REG_WRITE: Byte = 0x04 // Register the Instruction Packet to a standby status;
        private const val ACTION: Byte = 0x05 // Execute the Packet that was registered beforehand using REQ_WRITE
        private const val FACTORY_RESET: Byte = 0x06 // Reset the Control Table to its initial factory default settings
        private const val REBOOT: Byte = 0x08 // Instruction to reboot the Device
        private const val CLEAR: Byte = 0x10 // Instruction to reset certain information
        private const val STATUS_RETURN: Byte = 0x55 // Return Instruction for the Instruction Packet
        private const val SYNC_READ =
            0x82.toByte() // For multiple devices, Instruction to read data from the same Address with the same length at once
        private const val SYNC_WRITE =
            0x83.toByte() // For multiple devices, Instruction to write data on the same Address with the same length at once
        private const val BULK_READ =
            0x92.toByte() // For multiple devices, Instruction to read data from different Addresses with different lengths at once 
        private const val MAX_HIP_X = 190.0
        private const val MIN_HIP_Z = -8.0

        /**
         * Test using example in Robotis documentation for WRITE command and status, 5.3.3.2 and 5.3.3.3.
         * http://emanual.robotis.com/docs/en/dxl/protocol2
         */
        @JvmStatic
        fun main(args: Array<String>) {
            // Protocol 1
            val dxl = DxlMessage()
            var bytes = ByteArray(8)
            dxl.setHeader(bytes, 0x01)
            bytes[3] = 4 // Bytes past this field.
            bytes[4] = READ
            bytes[5] = 0x2B
            bytes[6] = 0x1
            dxl.setChecksum(bytes)
            // Should be CC
            println("READ  with checksum: " + dxl.dump(bytes))

            // Protocol 1
            bytes = dxl.bytesToBroadcastPing()
            // Checksum should be FE
            println("PING (1)  with checksum: " + dxl.dump(bytes))

            // Protocol 1
            // Sync write
            bytes = ByteArray(18)
            bytes[0] = 0xFF.toByte()
            bytes[1] = 0xFF.toByte()
            bytes[2] = 0xFE.toByte()
            bytes[3] = 0x0E.toByte()
            bytes[4] = SYNC_WRITE
            bytes[5] = 0x1E.toByte()
            bytes[6] = 0x04.toByte()
            bytes[7] = 0x00.toByte()
            bytes[8] = 0x10.toByte()
            bytes[9] = 0x00.toByte()
            bytes[10] = 0x50.toByte()
            bytes[11] = 0x01.toByte()
            bytes[12] = 0x01.toByte()
            bytes[13] = 0x20.toByte()
            bytes[14] = 0x02.toByte()
            bytes[15] = 0x60.toByte()
            bytes[16] = 0x03.toByte()
            dxl.setChecksum(bytes)
            // Checksum should be 67
            println("SYNC WRITE  with checksum: " + dxl.dump(bytes))
        }
    }
}