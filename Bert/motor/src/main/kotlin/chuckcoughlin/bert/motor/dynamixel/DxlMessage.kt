/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.motor.dynamixel

import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.motor.dynamixel.DxlConversions.CLSS
import chuckcoughlin.bert.motor.dynamixel.DxlConversions.LOGGER
import chuckcoughlin.bert.sql.db.Database
import com.google.gson.GsonBuilder
import java.util.logging.Logger

/**
 * This object contains utility methods used to create and interpret different varieties \
 * of Dynamixel serial messages. Code is derived from Pypot dynamixel.v2.py and the Dynamixel
 * documentation at http://emanual.robotis.com. Applies to MX64, MX28, AX12A models.
 * The documentation is unclear about the protocol version for AX-12 models, but it appears
 * to be Protocol 2.0. We have coded on this assumption.
 */
object DxlMessage {

    /**
     * Iterate through the list of motor configurations to determine which, if any, are outside the max-min
     * angle ranges. For those outside, move the position to a legal value.
     * WARNING: SYNC_WRITE requests do not generate responses.
     * Discount any current readings of zero, it probably means that the motor positions were never evaluated.
     * @param configurations a list of motor configuration objects
     * @return list of byte arrays with bulk read plus extras for any AX-12.
     */
    fun byteArrayListToInitializePositions(configurationsByJoint: Map<Joint, MotorConfiguration>): List<ByteArray> {
        val outliers: MutableList<MotorConfiguration> = ArrayList<MotorConfiguration>() // Will hold the joints that need moving.
        mostRecentTravelTime = 0
        for (mc in configurationsByJoint.values) {
            val pos: Double = mc.angle
            if (pos == 0.0) {
                LOGGER.info(String.format("%s.byteArrayListToInitializePositions: %s never evaluated, ignored",
                    CLSS,mc.joint.name))
            }
            else if (pos > mc.maxAngle ) {
                LOGGER.info(String.format("%s.byteArrayListToInitializePositions: %s out-of-range at %2.0f (max=%2.0f)",
                        CLSS, mc.joint.name, pos, mc.maxAngle))
                mc.angle = mc.maxAngle
                outliers.add(mc)
                if (mc.travelTime > mostRecentTravelTime) mostRecentTravelTime = mc.travelTime
            }
            else if (pos < mc.minAngle ) {
                LOGGER.info(String.format("%s.byteArrayListToInitializePositions: %s out-of-range at %2.0f (min=%2.0f)",
                        CLSS, mc.joint.name, pos, mc.minAngle))
                mc.angle = mc.minAngle
                outliers.add(mc)
                if (mc.travelTime > mostRecentTravelTime) mostRecentTravelTime = mc.travelTime
            }
        }
        // Add heuristics to avoid some common entanglements. Hip is only present in lower controller
        // No knock-knees
        var leftHip: MotorConfiguration? = configurationsByJoint[Joint.LEFT_HIP_X]
        if (leftHip != null) {
            if (leftHip.angle > HIP_X_LIMIT) {
                leftHip.angle = HIP_X_LIMIT
                outliers.add(leftHip)
            }
        }
        var rightHip: MotorConfiguration? = configurationsByJoint[Joint.RIGHT_HIP_X]
        if( rightHip!=null) {
            if (rightHip.angle > HIP_X_LIMIT) {
                rightHip.angle = HIP_X_LIMIT
                outliers.add(rightHip)
            }
        }
        // No pidgin toes
        leftHip = configurationsByJoint[Joint.LEFT_HIP_X]
        if( leftHip!=null ) {
            rightHip = configurationsByJoint[Joint.LEFT_HIP_X]
            if (leftHip.angle < HIP_Z_LIMIT) {
                leftHip.angle = HIP_Z_LIMIT
                outliers.add(leftHip)
            }
        }
        if( rightHip!=null) {
            if (rightHip.angle < HIP_Z_LIMIT) {
                rightHip.angle = HIP_Z_LIMIT
                outliers.add(rightHip)
            }
        }
        val messages: MutableList<ByteArray> = mutableListOf<ByteArray>()
        val pc = outliers.size
        // Positions
        if (pc > 0) {
            val len = 3 * pc + 8 //  3 bytes per motor + address + byte count + header + checksum
            val bytes = ByteArray(len)
            setSyncWriteHeader(bytes)
            bytes[3] = (len - 4).toByte()
            bytes[4] = SYNC_WRITE
            bytes[5] = DxlConversions.addressForGoalProperty(JointDynamicProperty.ANGLE)
            bytes[6] = 0x2 // 2 bytes
            var index = 7
            for (mc in outliers) {
                LOGGER.info(String.format("%s.byteArrayListToInitializePositions: set position for %s to %2.0f",
                        CLSS,mc.joint.name,mc.angle))
                val dxlValue = DxlConversions.dxlValueForProperty(JointDynamicProperty.ANGLE, mc, mc.angle)
                bytes[index] = mc.id.toByte()
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
     * @param property the desired joint property
     * @return list of byte arrays with bulk read plus extras for any AX-12.
     */
    fun byteArrayListToListProperty(property: JointDynamicProperty, configurations: Collection<MotorConfiguration>): List<ByteArray> {
        val messages: MutableList<ByteArray> = mutableListOf<ByteArray>()
        var count = configurations.size // Number of motors, less AX-12
        for (mc in configurations) {
            if (mc.type.equals(DynamixelType.AX12)) {
                val length = 4 // Remaining bytes past length including checksum
                val bytes = ByteArray(length + 4) // Account for header and length
                setHeader(bytes, mc.id)
                bytes[3] = length.toByte()
                bytes[4] = READ
                bytes[5] = DxlConversions.addressForPresentProperty(property)
                bytes[6] = DxlConversions.dataBytesForProperty(property)
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
            if (mc.type.equals(DynamixelType.AX12)) continue
            bytes[addr] = DxlConversions.dataBytesForProperty(property)
            bytes[addr + 1] = mc.id.toByte()
            bytes[addr + 2] = DxlConversions.addressForPresentProperty(property)
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
    fun byteArrayToSetProperty(map: Map<Joint, MotorConfiguration>, property: JointDynamicProperty): ByteArray {
        // First count all the joints in the limb
        val count = map.size
        var bytes: ByteArray = ByteArray(0)
        var dxlValue:Int
        if (count > 0) {
            val len =
                (DxlConversions.dataBytesForProperty(property) + 1) * count + 8 //  2 or 3 bytes per motor + address + byte count + header + checksum
            bytes = ByteArray(len)
            setSyncWriteHeader(bytes)
            bytes[3] = (len - 4).toByte()
            bytes[4] = SYNC_WRITE
            bytes[5] = DxlConversions.addressForGoalProperty(property)
            bytes[6] = DxlConversions.dataBytesForProperty(property)
            var index = 7
            for (mc in map.values) {
                bytes[index] = mc.id.toByte()
                if (property.equals(JointDynamicProperty.TORQUE)) {
                    dxlValue = DxlConversions.dxlValueForProperty(JointDynamicProperty.TORQUE, mc, mc.torque)
                    bytes[index + 1] = (dxlValue and 0xFF).toByte()
                    bytes[index + 2] = (dxlValue shr 8).toByte()
                }
                else if (property.equals(JointDynamicProperty.SPEED)) {
                    dxlValue = DxlConversions.dxlValueForProperty(JointDynamicProperty.SPEED, mc, mc.speed)
                    bytes[index + 1] = (dxlValue and 0xFF).toByte()
                    bytes[index + 2] = (dxlValue shr 8).toByte()
                }
                else if (property.equals(JointDynamicProperty.STATE)) {
                    dxlValue = DxlConversions.dxlValueForProperty(JointDynamicProperty.STATE,
                        mc,if (mc.isTorqueEnabled) 1.0 else 0.0
                    )
                    bytes[index + 1] = dxlValue.toByte()
                }
                index = index + 1 + DxlConversions.dataBytesForProperty(property)
            }
            setChecksum(bytes)
        }
        return bytes
    }

    /**
     * A pose consists of any or all position values for the specified joints. The supplied map contains joints for
     * the single relevant controller. The database query returns values for all controllers. Skip any that do not apply.
     * There is a hardware limit of 143 bytes for each array (shouldn't be a problem).
     *
     * Skip any motors that are already at the desired conditions. This has a significant performance effect.
     * We set speed and torque per the motor configurations for each joint that we move.
     *
     * WARNING: SYNC_WRITE requests, apparently, do not generate responses.
     * @param pose name of the pose to be set
     * @param map of the motor configurations to be changed for subject controller keyed by joint name
     * @return 3 byte arrays to drive torques, speeds and finally the positions as required by the pose
     */
    fun byteArrayListToSetPose(poseid: Long,map: Map<Joint, MotorConfiguration>): List<ByteArray> {
        LOGGER.info(String.format("%s.byteArrayListToSetPose: pose = %d",CLSS,poseid))
        // These maps contain joints for the subject limbs
        val torques: Map<Joint, Double> = Database.getPoseJointTorques( poseid, map)
        val speeds: Map<Joint, Double> = Database.getPoseJointSpeeds(poseid, map)
        val angles: Map<Joint, Double> = Database.getPoseJointPositions(poseid,map)
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
            bytes[5] = DxlConversions.addressForGoalProperty(JointDynamicProperty.TORQUE)
            bytes[6] = 0x2 // 2 bytes
            var index = 7
            var isChanged = false
            for (key in map.keys) {
                if( torques[key]==null ) continue    // joint not being set
                if( map[key]==null )     continue
                val mc: MotorConfiguration = map[key]!!
                if( mc.torque==torques[key]) continue
                isChanged = true
                val dxlValue = DxlConversions.dxlValueForProperty(JointDynamicProperty.TORQUE, mc, torques[key]!!)
                bytes[index] = mc.id.toByte()
                bytes[index + 1] = (dxlValue and 0xFF).toByte()
                bytes[index + 2] = (dxlValue shr 8).toByte()
                mc.torque = torques[key]!! // percent of max
                index = index + 3
            }
            if( isChanged ) {
                setChecksum(bytes)
                messages.add(bytes)
            }
        }
        val sc = speeds.size
        // Speed
        if (sc > 0) {
            val len = 3 * sc + 8 //  3 bytes per motor + address + byte count + header + checksum
            val bytes = ByteArray(len)
            setSyncWriteHeader(bytes)
            bytes[3] = (len - 4).toByte()
            bytes[4] = SYNC_WRITE
            bytes[5] = DxlConversions.addressForGoalProperty(JointDynamicProperty.SPEED)
            bytes[6] = 0x2 // 2 bytes
            var index = 7
            var isChanged = false
            for (key in map.keys) {
                if( speeds[key]==null ) continue
                if( map[key]==null )  continue
                val mc: MotorConfiguration = map[key]!!
                if( mc.speed==speeds[key]!! ) continue
                isChanged = true
                val dxlValue = DxlConversions.dxlValueForProperty(JointDynamicProperty.SPEED, mc, speeds[key]!!)
                bytes[index] = mc.id.toByte()
                bytes[index + 1] = (dxlValue and 0xFF).toByte()
                bytes[index + 2] = (dxlValue shr 8).toByte()
                mc.speed = speeds[key]!! // percent of max
                index = index + 3
            }
            if( isChanged ) {
                setChecksum(bytes)
                messages.add(bytes)
            }
        }
        val pc = angles.size
        // Positions - correct any that are outside legal limits.
        if (pc > 0) {
            mostRecentTravelTime = 0
            val len = 3 * pc + 8 //  3 bytes per motor + address + byte count + header + checksum
            val bytes = ByteArray(len)
            setSyncWriteHeader(bytes)
            bytes[3] = (len - 4).toByte()
            bytes[4] = SYNC_WRITE
            bytes[5] = DxlConversions.addressForGoalProperty(JointDynamicProperty.ANGLE)
            bytes[6] = 0x2 // 2 bytes
            var index = 7
            var isChanged = false
            for (key in map.keys) {
                if( map[key]==null ) continue
                val mc: MotorConfiguration = map[key]!!
                if( angles[key] == null )  continue
                if( mc.angle==angles[key]!! ) continue
                isChanged = true
                LOGGER.info(String.format("%s.byteArrayListToSetPose: position for %s to %2.0f",CLSS,key,angles.get(key)));
                mc.angle = angles[key]!!
                if(mc.angle>mc.maxAngle)  {
                    LOGGER.info(String.format("%s.byteArrayListToSetPose: pose %d at %s has %2.0f greater than the maximium %2.0f",
                        CLSS,poseid,mc.joint.name,mc.angle,mc.maxAngle));
                    mc.angle = mc.maxAngle
                }
                else if(mc.angle<mc.minAngle) {
                    LOGGER.info(String.format("%s.byteArrayListToSetPose: pose %d at %s has angle %2.0f less than the minimium of %2.0f",
                        CLSS,poseid,mc.joint.name,mc.angle,mc.minAngle));
                    mc.angle = mc.minAngle
                }
                val dxlValue = DxlConversions.dxlValueForProperty(JointDynamicProperty.ANGLE, mc, mc.angle)
                bytes[index] = mc.id.toByte()
                bytes[index + 1] = (dxlValue and 0xFF).toByte()
                bytes[index + 2] = (dxlValue shr 8).toByte()

                if (mc.travelTime > mostRecentTravelTime) mostRecentTravelTime = mc.travelTime
                index = index + 3
            }
            if( isChanged ) {
                setChecksum(bytes)
                messages.add(bytes)
            }
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
     * @param property the desired property
     * @return byte array with command to read the property
     */
    fun bytesToGetProperty(id: Int, property: JointDynamicProperty): ByteArray {
        val length = 4 // Remaining bytes past length including checksum
        val bytes = ByteArray(length + 4) // Account for header and length
        setHeader(bytes, id)
        bytes[3] = length.toByte()
        bytes[4] = READ
        bytes[5] = DxlConversions.addressForPresentProperty(property)
        bytes[6] = DxlConversions.dataBytesForProperty(property)
        setChecksum(bytes)
        return bytes
    }

    /**
     * Create a serial message to write a goal for the motor. Recognized properties are:
     * position, speed, torque and torque_enable. All except torque enable are two byte parameters.
     * @param id of the motor
     * @param property the desired property
     * @return byte array with command to set the property
     */
    fun bytesToSetProperty(mc: MotorConfiguration, property: JointDynamicProperty, value: Double): ByteArray {
        val dxlValue = DxlConversions.dxlValueForProperty(property, mc, value)
        val length = 3 + DxlConversions.dataBytesForProperty(property) // Remaining bytes past length including checksum
        val bytes = ByteArray(length + 4) // Account for header and length
        setHeader(bytes, mc.id)
        bytes[3] = length.toByte()
        bytes[4] = WRITE
        bytes[5] = DxlConversions.addressForGoalProperty(property)
        if (DxlConversions.dataBytesForProperty(property).toInt() == 2) {
            bytes[6] = (dxlValue and 0xFF).toByte()
            bytes[7] = (dxlValue shr 8).toByte()
        }
        else {
            bytes[6] = dxlValue.toByte()
        }
        setChecksum(bytes)
        if (property.equals(JointDynamicProperty.ANGLE)) {
            mc.angle = value
            mostRecentTravelTime = mc.travelTime
        }
        else if (property.equals(JointDynamicProperty.SPEED)) {
            mc.speed = value
        }
        else if (property.equals(JointDynamicProperty.STATE)) {
            mc.isTorqueEnabled = (if (value == 0.0) false else true)
        }
        else if (property.equals(JointDynamicProperty.TEMPERATURE)) {
            mc.temperature = value
        }
        else if (property.equals(JointDynamicProperty.TORQUE))
            mc.torque = value
        return bytes
    }

    /**
     * Create a string suitable for printing and debugging.
     * @param bytes
     * @return a formatted string of the bytes as hex digits.
     */
    fun dump(bytes: ByteArray): String {
        val sb = StringBuffer()
        var index = 0
        if( bytes.size>0 ) {
            while( index < bytes.size ) {
                //if( bytes[index]=='\0') break;
                sb.append(String.format("%02X", bytes[index]))
                sb.append(" ")
                index++
            }

            // Add the buffer length
            sb.append("(")
            sb.append(bytes.size)
            sb.append(")")
        }
        else {
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
    fun ensureLegalStart(bytes: ByteArray): ByteArray {
        var i = 0
        while (i < bytes.size - 2) {
            if (bytes[i] == 0xFF.toByte() &&
                bytes[i] == 0xFF.toByte() ) {
                return if (i == 0) {
                    bytes
                }
                else {
                    val copy = ByteArray(bytes.size - i)
                    System.arraycopy(bytes, i, copy, 0, copy.size)
                    LOGGER.warning( String.format( "%s.ensureLegalStart: cut %d bytes to provide legal msg",
                            CLSS,i))
                    copy
                }
            }
            i++
        }
        return bytes
    }

    /**
     * The only interesting information in a status message from a write
     * to a single device is the error code.
     * @param bytes
     * @return
     */
    fun errorMessageFromStatus(bytes: ByteArray): String {
        var msg: String = ""
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
     * @param bottle a MessageBottle in which we set properties
     * @param bytes status response from the controller
     */
    @Synchronized
    fun updateGoalsFromBytes(mc: MotorConfiguration, bottle: MessageBottle, bytes: ByteArray) {
        if (verifyHeader(bytes)) {
            if(DEBUG) {
                val msg = String.format("%s.updateGoalsFromBytes: %s", CLSS, dump(bytes))
                LOGGER.info(msg)
            }
            val err = bytes[4]
            val gson = GsonBuilder().setPrettyPrinting().create()
            var motorValues = mutableListOf<JointPropertyValue>()
            var property: JointDynamicProperty = JointDynamicProperty.ANGLE
            val v1 = DxlConversions.valueForProperty(property, mc, bytes[5], bytes[6])
            val t1 = DxlConversions.textForProperty(property, mc, bytes[5], bytes[6])
            motorValues.add(JointPropertyValue(bottle.joint,property,v1))
            mc.angle = v1
            property = JointDynamicProperty.SPEED // Non-directional
            var v2 = DxlConversions.valueForProperty(property, mc, bytes[7], bytes[8])
            val t2 = DxlConversions.textForProperty(property, mc, bytes[7], bytes[8])
            motorValues.add(JointPropertyValue(bottle.joint,property,v2))
            v2 = v2 * 100.0 / DxlConversions.velocity.get(mc.type)!! // Convert to percent
            mc.speed = v2
            property = JointDynamicProperty.TORQUE // Non-directional
            var v3 = DxlConversions.valueForProperty(property, mc, bytes[9], bytes[10])
            val t3 = DxlConversions.textForProperty(property, mc, bytes[9], bytes[10])
            motorValues.add(JointPropertyValue(bottle.joint,property,v3))
            v3 = v2 * 100.0 / DxlConversions.torque.get(mc.type)!! // Convert to percent
            mc.torque = v3
            val text = String.format("Goal angle, speed and torque are : %s, %s, %s", t1, t2, t3)
            if (err.toInt() == 0) {
                LOGGER.info(text)
            }
            else {
                val errText = String.format("%s.updateGoalsFromBytes: message returned error %d (%s)",
                    CLSS,err,descriptionForError(err))
                bottle.error = errText
                LOGGER.severe(errText)
            }
            bottle.text = gson.toJson(motorValues)
        }
        else {
            val msg = String.format("%s.updateGoalsFromBytes: Illegal message: %s", CLSS, dump(bytes))
            bottle.error = msg
            LOGGER.severe(msg)
        }
    }

    /**
     * Analyze a response buffer returned from a request for EEPROM limits for a motor. Limit
     * parameters are: angles, temperature, voltage and torque. Of these we extract only the
     * angles and torque. These are NOT corrected for offset or orientation.
     * @param type the model of the motor
     * @param isDirect the orientation of the motor
     * @param bottle a MessageBottle in which we set properties
     * @param bytes status response from the controller
     */
    @Synchronized
    fun updateLimitsFromBytes(mc: MotorConfiguration, bottle: MessageBottle, bytes: ByteArray) {
        mc.isDirect = true
        mc.offset = 0.0
        if (verifyHeader(bytes)) {
            // val msg = String.format("%s.updateLimitsFromBytes: %s", CLSS, dump(bytes))
            // val id = bytes[2].toInt()
            val err = bytes[4]
            val gson = GsonBuilder().setPrettyPrinting().create()
            var motorValues = mutableListOf<JointPropertyValue>()
            var property: JointDynamicProperty = JointDynamicProperty.MAXIMUMANGLE // CW
            val v1 = DxlConversions.valueForProperty(property, mc, bytes[5], bytes[6])
            val t1 = DxlConversions.textForProperty(property, mc, bytes[5], bytes[6])
            motorValues.add(JointPropertyValue(bottle.joint,property,v1))
            property = JointDynamicProperty.MINIMUMANGLE // CCW
            val v2 = DxlConversions.valueForProperty(property, mc, bytes[7], bytes[8])
            val t2 = DxlConversions.textForProperty(property, mc, bytes[7], bytes[8])
            motorValues.add(JointPropertyValue(bottle.joint,property,v2))
            property = JointDynamicProperty.TORQUE // Non-directional
            val v3 = DxlConversions.valueForProperty(property, mc, bytes[12], bytes[13])
            val t3 = DxlConversions.textForProperty(property, mc, bytes[12], bytes[13])
            motorValues.add(JointPropertyValue(bottle.joint,property,v3))
            val text = String.format("min, max angle and torque limits are : %s, %s, %s", t2, t1, t3)
            if (err.toInt() == 0) {
                LOGGER.info(text)
            }
            else {
                val msg = String.format("%s.updateLimitsFromBytes: message returned error %d (%s)",
                    CLSS,err,descriptionForError(err))
                bottle.error = msg
                LOGGER.severe(msg)
            }
            bottle.text = gson.toJson(motorValues)
        }
        else {
            val msg = String.format("%s.updateLimitsFromBytes: Illegal message: %s", CLSS, dump(bytes))
            bottle.error = msg
            LOGGER.severe(msg)
        }
    }

    /**
     * Analyze a response buffer for some parameter of a motor. Augment the
     * supplied Properties with the result (possibly an error).
     * @param property the requested parameter
     * @param type the model of the motor
     * @param isDirect the orientation of the motor
     * @param props properties from a MessageBottle
     * @param bytes status response from the controller
     */
    @Synchronized
    fun updateParameterFromBytes(property: JointDynamicProperty,mc: MotorConfiguration,
            bottle: MessageBottle,bytes: ByteArray) {
        if (verifyHeader(bytes)) {
            // var msg = String.format("%s.updateParameterFromBytes: %s", CLSS, dump(bytes))
            // val id = bytes[2].toInt()
            val err = bytes[4]
            val value = DxlConversions.valueForProperty(property, mc, bytes[5], bytes[6])
            val text = DxlConversions.textForProperty(property, mc, bytes[5], bytes[6])
            if (err.toInt() == 0) {
                bottle.joint = mc.joint
                bottle.jointDynamicProperty = property
                bottle.value = value
                bottle.text = text
                LOGGER.info(String.format("%s.updateParameterFromBytes: %s %s=%.2f",
                        CLSS,mc.joint,property,value)
                )
            }
            else {
                val msg = String.format("%s.updateParameterFromBytes: message returned error %d (%s)",
                    CLSS,err, descriptionForError(err))
                bottle.error = msg
                LOGGER.severe(msg)
            }
        }
        else {
            val msg = String.format("%s.updateParameterFromBytes: Illegal message: %s", CLSS, dump(bytes))
            bottle.error = msg
            LOGGER.severe(msg)
        }
    }

    /**
     * Analyze the response buffer for the indicated motor parameter and
     * update the supplied position map accordingly. There may be several responses concatenated.
     * We assume that the parameter is 2 bytes. Log errors, there's not much else we can do.
     * @param property the name of the Joint property being handled
     * @param configurations a map of motor configurations by id
     * @param bytes status response from the controller
     * @param parameters an array of positions by id, supplied. This is augmented by the method.
     */
    @Synchronized
    fun updateParameterArrayFromBytes(property: JointDynamicProperty,configurations: Map<Int, MotorConfiguration>,
            bytes: ByteArray, parameters: MutableMap<Int, String> ) {
        var msg: String
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
                    val param = DxlConversions.valueForProperty(property, mc, bytes[index + 5], bytes[index + 6])
                    parameters[id] = String.format("%.2f", param)
                    when(property) {
                        JointDynamicProperty.ANGLE -> mc.angle = param
                        JointDynamicProperty.TORQUE   -> mc.torque = param
                        JointDynamicProperty.MAXIMUMANGLE -> mc.maxAngle = param
                        JointDynamicProperty.MINIMUMANGLE -> mc.minAngle = param
                        JointDynamicProperty.RANGE -> {}
                        JointDynamicProperty.SPEED        -> mc.speed = param
                        JointDynamicProperty.STATE        -> {}
                        JointDynamicProperty.TEMPERATURE  -> mc.temperature = param
                        JointDynamicProperty.VOLTAGE      -> mc.voltage = param
                        JointDynamicProperty.NONE         -> {}
                    }
                    LOGGER.info(String.format("%s.updateParameterArrayFromBytes: %s %s=%2.0f",
                            CLSS,mc.joint.name,property,param))
                }
                else if (err.toInt() != 0) {
                    msg = String.format( "%s.updateParameterArrayFromBytes: motor %d returned error %d (%s)",
                        CLSS, id, err, descriptionForError(err))
                    LOGGER.severe(msg)
                }
                else if (mc == null) {
                    msg = String.format(
                        "%s.updateParameterArrayFromBytes: motor %d not supplied in motor configurations",
                        CLSS,id,dump(bytes))
                    LOGGER.severe(msg)
                }
                else if (bytes.size <= index + 6) {
                    msg = String.format("%s.updateParameterArrayFromBytes: motor %d input truncated (%s)",
                        CLSS,id,)
                    LOGGER.severe(msg)
                }
                else {
                    LOGGER.severe(String.format("%s.updateParameterArrayFromBytes: programming error at id=%d",
                        CLSS,id,dump(bytes)))
                }
            }
            else {
                LOGGER.severe( String.format("%s.updateParameterArrayFromBytes: Header not found: %s",
                        CLSS,dump(bytes)))
            }
            index = index + length
        }
    }

    // ===================================== Private Methods =====================================
    // Return a string describing the error. We only check one bit.
    private fun descriptionForError(err: Byte): String {
        var description = "Unrecognized error"
        if (err.toInt() and 0x01 != 0x00) {
            description = "an instruction error"
        }
        else if (err.toInt() and 0x02 != 0x00) {
            description = "an overload error"
        }
        else if (err.toInt() and 0x04 != 0x00) {
            description = "an incorrect checksum"
        }
        else if (err.toInt() and 0x08 != 0x00) {
            description = "a range error"
        }
        else if (err.toInt() and 0x10 != 0x00) {
            description = "overheating"
        }
        else if (err.toInt() and 0x20 != 0x00) {
            description = "a position outside angle limits"
        }
        else if (err.toInt() and 0x40 != 0x00) {
            description ="an input voltage outside the acceptable range"
        }
        return description
    }

    // Set the header up until the length field. The header includes the device ID.
    // Protocol 1. 3 bytes
    public fun setHeader(bytes: ByteArray, id: Int) {
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

    /**
     * As each method that generates motor motions is invoked, it calculates time to execute the movement.
     * The result is stored as a static parameter, good only until the next method is run.
     * @return the maximum travel time as calculated by the most recent byte syntax generator. Time ~msecs.
     */
    var mostRecentTravelTime: Long = 0
        private set
    const val CLSS = "DxlMessage"
    val LOGGER = Logger.getLogger(CLSS)
    val DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_MOTOR)

    // Constants for the instructions
    const val BROADCAST_ID = 0xFE.toByte() // ID to transmit to all devices connected to port
    const val PING: Byte = 0x01 // Instruction that checks whether the Packet has arrived
    const val READ: Byte = 0x02 // Instruction to read data from the Device
    const val WRITE: Byte = 0x03 // Instruction to write data on the Device const val REG_WRITE: Byte = 0x04 // Register the Instruction Packet to a standby status;
    const val ACTION: Byte = 0x05 // Execute the Packet that was registered beforehand using REQ_WRITE
    const val FACTORY_RESET: Byte = 0x06 // Reset the Control Table to its initial factory default settings
    const val REBOOT: Byte = 0x08 // Instruction to reboot the Device
    const val CLEAR: Byte = 0x10 // Instruction to reset certain information
    const val STATUS_RETURN: Byte = 0x55 // Return Instruction for the Instruction Packet
    const val SYNC_READ = 0x82.toByte() // For multiple devices, Instruction to read data from the same Address with the same length at once
    const val SYNC_WRITE = 0x83.toByte() // For multiple devices, Instruction to write data on the same Address with the same length at once
    const val BULK_READ = 0x92.toByte() // For multiple devices, Instruction to read data from different Addresses with different lengths at once
    const val HIP_X_LIMIT = 190.0         // Reasonable hip limit
    const val HIP_Z_LIMIT = -8.0
}