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


    // Broadcast intent categories
    public static final String CATEGORY_FACILITY_STATE = "FacilityState";
    public static final String CATEGORY_SPOKEN_TEXT   = "SpokenText";

    // "extra" data keys
    public static final String KEY_TIERED_FACILITY = "TieredFacility";
    public static final String KEY_FACILITY_STATE = "FacilityState";
    public static final String KEY_SPOKEN_TEXT   = "SpokenText";
}