/*
 * Copyright (C) 2017 Chuck Coughlin
 *  (MIT License)
 */
package chuckcoughlin.bert.common;

/**
 * This class contains static strings used throughout
 * the spplication.
 */
public class BertConstants {
    // Database configuration
    public static final String DB_NAME = "BertSpeak.db";
    public static final int DB_VERSION = 1;
    // These are the Settings table columns
    public static final String SETTINGS_NAME  = "name";
    public static final String SETTINGS_VALUE = "value";


    // Name of the main node on the tablet
    public static final String MAIN_NODE_NAME = "tablet_node_main";

    // Number of log messages to store/display
    public static final int NUM_LOG_MESSAGES = 100;


    // Dialog transaction key
    public static final String DIALOG_TRANSACTION_KEY = "dialog";
}