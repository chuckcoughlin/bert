/*
 * Copyright (C) 2017 Chuck Coughlin
 *  (MIT License)
 */
package chuckcoughlin.bert.common;

/**
 * This class contains static strings used throughout
 * the application.
 */
public class BertConstants {
    // Database configuration
    public static final String DB_NAME = "BertSpeak.db";
    public static final int DB_VERSION = 1;
    // These are the Settings table columns
    public static final String SETTINGS_NAME  = "name";
    public static final String SETTINGS_VALUE = "value";

    // These are the parameter names in the Settings table
    public static final String BERT_SERVER="Server";
    public static final String BERT_PORT="Port";
    public static final String BERT_PAIRED_DEVICE="Paired Device";
    public static final String BERT_SERVICE_UUID="Paired Device";
    public static final String BERT_SIMULATED_CONNECTION="Simulated Connection";

    // These are the default values for the settings
    public static final String BERT_SERVER_HINT="192.168.1.20";
    public static final String BERT_PORT_HINT="1-30";
    public static final String BERT_PAIRED_DEVICE_HINT="bert_humanoid";
    public static final String BERT_SERVICE_UUID_HINT="33001101-0000-2000-8080-00815FAB34FF";
    public static final String BERT_SIMULATED_CONNECTION_HINT="false";

    // Intent observer recognizer
    public static final String ACTION_FACILITY_STATE = "FacilityState";

    // Notifications
    public static final String NOTIFICATION_CHANNEL_ID   = "speechServiceChannel";
    public static final String NOTIFICATION_CHANNEL_NAME = "Spoken Text Channel";

    // Name of the main node on the tablet
    public static final String MAIN_NODE_NAME = "tablet_node_main";

    // Number of log messages to store/display
    public static final int NUM_LOG_MESSAGES = 100;


    // Dialog transaction key
    public static final String DIALOG_TRANSACTION_KEY = "dialog";
    // For messages from robot (Does not include semi-colon)
    public static final int HEADER_LENGTH = 3;
}