/*
 * Copyright (C) 2017 Chuck Coughlin
 *  (MIT License)
 */
package chuckcoughlin.bert.service;

/**
 * This class contains static strings used in conjunction
 * with the Voice Service.
 */
public class VoiceConstants {

    // Broadcast receiver actions
    public static final String RECEIVER_SERVICE_STATE = "ServiceState";
    public static final String RECEIVER_SPOKEN_TEXT  = "SpokenText";

    // Broadcast intent categories
    public static final String CATEGORY_SERVICE_STATE = "ServiceState";
    public static final String CATEGORY_SPOKEN_TEXT   = "SpokenText";

    // "extra" data keys
    public static final String KEY_SERVICE_ACTION = "ServiceAction";
    public static final String KEY_SERVICE_STATE = "ServiceState";
    public static final String KEY_SPOKEN_TEXT   = "SpokenText";
}