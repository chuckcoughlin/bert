/*
 * Copyright (C) 2022 Chuck Coughlin
 *  (MIT License)
 */
package chuckcoughlin.bertspeak.service

/**
 * This class contains static strings used in conjunction
 * with the Voice Service.
 */
object VoiceConstants {
    // Broadcast intent categories
    const val CATEGORY_FACILITY_STATE = "FacilityState"
    const val CATEGORY_SPOKEN_TEXT = "SpokenText"

    // "extra" data keys
    const val KEY_TIERED_FACILITY = "TieredFacility"
    const val KEY_FACILITY_STATE = "FacilityState"
    const val KEY_SPOKEN_TEXT = "SpokenText"
}