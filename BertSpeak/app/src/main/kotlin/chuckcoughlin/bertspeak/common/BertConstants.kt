/*
 * Copyright (C) 2022 Chuck Coughlin
 *  (MIT License)
 */
package chuckcoughlin.bertspeak.common

/**
 * This class contains static strings used throughout
 * the application.
 */
object BertConstants {
    // Database configuration
    const val DB_NAME = "BertSpeak.db"
    const val DB_VERSION = 1

    // These are the Settings table columns
    const val SETTINGS_NAME = "name"
    const val SETTINGS_VALUE = "value"

    // These are the parameter names in the Settings table
    const val BERT_SERVER = "Server"
    const val BERT_PORT = "Port"
    const val BERT_PAIRED_DEVICE = "Paired Device"
    const val BERT_SERVICE_UUID = "Paired Device"
    const val BERT_SIMULATED_CONNECTION = "Simulated Connection"

    // These are the default values for the settings
    const val BERT_SERVER_HINT = "192.168.1.20"
    const val BERT_PORT_HINT = "1-30"
    const val BERT_PAIRED_DEVICE_HINT = "bert_humanoid"
    const val BERT_SERVICE_UUID_HINT = "33001101-0000-2000-8080-00815FAB34FF"
    const val BERT_SIMULATED_CONNECTION_HINT = "false"

    // Intent observer recognizer
    const val ACTION_FACILITY_STATE = "FacilityState"

    // For saved UI state in a bundle
    const val BUNDLE_FROZEN = "Frozen"

    // Notifications
    const val NOTIFICATION_CHANNEL_ID = "speechServiceChannel"
    const val NOTIFICATION_CHANNEL_NAME = "Spoken Text Channel"

    // Name of the main node on the tablet
    const val MAIN_NODE_NAME = "tablet_node_main"

    // Number of log messages to store/display
    const val NUM_LOG_MESSAGES = 40

    // Dialog transaction key
    const val DIALOG_TRANSACTION_KEY = "dialog"

    // For messages from robot (Does not include semi-colon)
    const val HEADER_LENGTH = 3
}