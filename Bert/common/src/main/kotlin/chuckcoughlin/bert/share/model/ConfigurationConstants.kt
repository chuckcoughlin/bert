/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.share.model

/**
 * Define strings found in the configuration file
 */
interface ConfigurationConstants {
    companion object {
        // Keys for properties
        const val PROPERTY_BLUESERVER_PORT = "blueserver"
        const val PROPERTY_CADENCE = "cadence"
        const val PROPERTY_CONTROLLER_NAME = "controller"
        const val PROPERTY_HOSTNAME = "hostname"
        const val PROPERTY_PROCESS_NAME = "process"
        const val PROPERTY_PROMPT = "prompt"
        const val PROPERTY_ROBOT_NAME = "name"
    }
}