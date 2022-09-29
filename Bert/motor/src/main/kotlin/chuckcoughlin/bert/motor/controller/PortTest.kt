/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.motor.controller

import jssc.SerialPort

/**
 * Exercise a port connected to the Dynamixels.
 */
class PortTest : Runnable, SerialPortEventListener {
    private val port: SerialPort
    private val dxl: DxlMessage

    init {
        port = SerialPort(DEVICE)
        dxl = DxlMessage()
    }

    override fun run() {
        var success = true
        try {
            success = port.closePort()
            println(String.format("PortTest.close: Success = %s", if (success) "true" else "false"))
        } catch (ignore: SerialPortException) {
        }
        try {
            // Open the port
            delay()
            success = port.openPort()
            println(String.format("PortTest.open: Success = %s", if (success) "true" else "false"))
            println(String.format("PortTest.open: isOpened = %s", if (port.isOpened()) "true" else "false"))

            // Configure the port
            delay()
            success = port.setParams(
                BAUD_RATE,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE,
                false,
                false
            )
            println(String.format("PortTest.setParams: Success = %s", if (success) "true" else "false"))
            delay()
            success = port.setEventsMask(SerialPort.MASK_RXCHAR)
            println(String.format("PortTest.setEventsMask: Success = %s", if (success) "true" else "false"))
            delay()
            success = port.purgePort(SerialPort.PURGE_RXCLEAR)
            success = success && port.purgePort(SerialPort.PURGE_TXCLEAR)
            println(String.format("PortTest.purgePort: Success = %s", if (success) "true" else "false"))
            delay()
            port.addEventListener(this)
        } catch (spe: SerialPortException) {
            println(
                java.lang.String.format(
                    "PortTest: Error opening/configuring port %s (%s)",
                    DEVICE,
                    spe.getLocalizedMessage()
                )
            )
        }
        // Write
        delay()
        var bytes: ByteArray = dxl.bytesToBroadcastPing()
        try {
            // Write the buffer
            success = port.writeBytes(bytes)
            println(
                String.format(
                    "PortTest.writeBytes: Success = %s writing %d bytes ",
                    if (success) "true" else "false",
                    bytes.size
                )
            )
        } catch (spe: SerialPortException) {
            println(
                java.lang.String.format(
                    "PortTest: Error writing %s (%s)",
                    dxl.dump(bytes),
                    spe.getLocalizedMessage()
                )
            )
        }

        // Read
        delay()
        try {
            // Read the port
            val incount: Int = port.getInputBufferBytesCount()
            val outcount: Int = port.getOutputBufferBytesCount()
            println(
                String.format(
                    "PortTest.getInputBufferBytesCount: %d bytes ready to read, %d bytes to write",
                    incount,
                    outcount
                )
            )
            if (incount > 0) {
                bytes = port.readBytes()
                if (bytes != null) println(String.format("PortTest.readBytes: Got %d bytes", bytes.size))
                println(String.format("PortTest.readBytes: %s", dxl.dump(bytes)))
            }
        } catch (spe: SerialPortException) {
            println(java.lang.String.format("PortTest: Error reading (%s)", spe.getLocalizedMessage()))
        }

        // Close
        delay()
        try {
            success = port.closePort()
            println(String.format("PortTest.close: Success = %s", if (success) "true" else "false"))
        } catch (spe: SerialPortException) {
            println(java.lang.String.format("PortTest.close: Error closing port (%s)", spe.getLocalizedMessage()))
        }
    }

    /**
     * Provide some time spacing between test steps
     */
    private fun delay() {
        try {
            Thread.sleep(1000)
        } catch (ignore: InterruptedException) {
        }
    }
    // ============================== SerialPortEventListener ===============================
    /**
     * Handle the response from the serial request.
     */
    fun serialEvent(event: SerialPortEvent?) {
        println("PotTest: Got a serial event ")
    }

    companion object {
        private const val BAUD_RATE = 1000000
        private const val DEVICE = "/dev/ttyACM0"

        /**
         * Open a port. Write and read.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val tester = Thread(PortTest())
            tester.start()
        }
    }
}