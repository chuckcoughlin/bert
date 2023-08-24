/**
 * Copyright 2019-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * Define strings found in the configuration file
 */
object ConfigurationConstants {
    // Keys for standard controller names
    const val CONTROLLER_DISPATCHER = "Dispatcher"

    // Keys for properties found in the main configuration file
    const val PROPERTY_CADENCE = "cadence"
    const val PROPERTY_CONTROLLER_NAME = "controller"
    const val PROPERTY_DEVICE = "device"
    const val PROPERTY_HOSTNAME = "hostname"
    const val PROPERTY_PORT = "port"
    const val PROPERTY_PROMPT = "prompt"
    const val PROPERTY_ROBOT_NAME = "name"
    const val PROPERTY_SOCKET = "socket"
    const val PROPERTY_UUID = "uuid"
    const val PROPERTY_NONE = "none"

    // Error values
    const val NO_CONTROLLER  = "Not Defined"
    const val NO_DEVICE      = "No Device"
    const val NO_PORT        = "No Port"
    const val NO_VALUE       = "No Value"
}