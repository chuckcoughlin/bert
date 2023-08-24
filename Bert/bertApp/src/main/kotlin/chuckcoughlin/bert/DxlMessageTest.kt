/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert

import chuckcoughlin.bert.motor.dynamixel.DxlMessage
import chuckcoughlin.bert.motor.dynamixel.DxlMessage.READ
import chuckcoughlin.bert.motor.dynamixel.DxlMessage.SYNC_WRITE

/**
 * This object contains utility methods used to create and interpret different varieties \
 * of Dynamixel serial messages. Code is derived from Pypot dynamixel.v2.py and the Dynamixel
 * documentation at http://emanual.robotis.com. Applies to MX64, MX28, AX12A models.
 * The documentation is unclear about the protocol version for AX-12 models, but it appears
 * to be Protocol 2.0. We have coded on this assumption.
 */
class DxlMessageTest {

}
/*
 * Test using example in Robotis documentation for WRITE command and status, 5.3.3.2 and 5.3.3.3.
  * http://emanual.robotis.com/docs/en/dxl/protocol2
 */
fun main(args: Array<String>) {
    // Protocol 1
    var bytes = ByteArray(8)
    DxlMessage.setHeader(bytes, 0x01)
    bytes[3] = 4 // Bytes past this field.
    bytes[4] = READ
    bytes[5] = 0x2B
    bytes[6] = 0x1
    DxlMessage.setChecksum(bytes)
    // Should be CC
    println("READ  with checksum: " + DxlMessage.dump(bytes))

    // Protocol 1
    bytes = DxlMessage.bytesToBroadcastPing()
    // Checksum should be FE
    println("PING (1)  with checksum: " + DxlMessage.dump(bytes))

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
    DxlMessage.setChecksum(bytes)
    // Checksum should be 67
    println("SYNC WRITE  with checksum: " + DxlMessage.dump(bytes))
}