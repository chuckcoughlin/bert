/*
 * Copyright (C) 2022-2024 Chuck Coughlin
 *  (MIT License)
 */
package chuckcoughlin.bertspeak.common

/**
 * This class contains static strings used throughout
 * the application.
 */
object BertConstants {
    // Database configuration - must be on a path that exists in Android
    const val DB_FILE_PATH = "/data/data/chuckcoughlin.bertspeak/BertSpeak.db"
    const val DB_NAME = "BertSpeak.db"
    const val DB_VERSION = 2

    // These are the Settings table columns
    const val SETTINGS_NAME = "name"
    const val SETTINGS_VALUE = "value"

    // These are the parameter names in the Settings table
    const val BERT_SERVER = "Server"
    const val BERT_PORT = "Port"
    const val BERT_PAIRED_DEVICE = "Paired Device"
    const val BERT_SIMULATED_CONNECTION = "Simulated Connection"
    const val BERT_VERSION = "Database Version"
    const val BERT_VOLUME = "Volume"

    // These are the default values for the settings
    const val BERT_SERVER_HINT = "10.0.0.42"
    const val BERT_PORT_HINT = "11046"
    const val BERT_PAIRED_DEVICE_HINT = "bert"
    const val BERT_VERSION_HINT = "2"
    const val BERT_VOLUME_HINT = "0.5"
    const val BERT_SERVICE_UUID_HINT = "33001101-0000-2000-8080-00815FAB34FF"
    const val BERT_SIMULATED_CONNECTION_HINT = "false"

    // For saved UI state in a bundle
    const val BUNDLE_FROZEN = "Frozen"

    // Undefined
    const val NO_DEVICE = "UNDEFINED_DEVICE"
    const val NO_ERROR  = "NO ERROR"
    const val NO_ID     = -1
    const val NO_JOB    = -1
    const val NO_NAME  = "NO_NAME"
    const val NO_TITLE  = "NO_TITLE"

    // Name of the main node on the tablet
    const val MAIN_NODE_NAME = "tablet_node_main"

    // Number of various types of messages to store/display
    const val NUM_JOINTS = 30
    const val NUM_LOG_MESSAGES = 40
    const val NUM_POSES = 40
    const val NUM_TRANSCRIPT_MESSAGES = 80

    // For messages from robot (Does not include semi-colon)
    const val HEADER_LENGTH = 3
}
