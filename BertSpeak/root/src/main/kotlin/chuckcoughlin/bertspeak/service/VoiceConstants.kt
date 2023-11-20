/*
 * Copyright (C) 2023 Chuck Coughlin
 *  (MIT License)
 */
package chuckcoughlin.bertspeak.service

/**
 * This class contains static strings used in conjunction
 * with the Voice Service.
 */
object VoiceConstants {
    // Broadcast intent categories
    const val CATEGORY_CONTROLLER_STATE = "ControllerState"
    const val CATEGORY_SPOKEN_TEXT = "SpokenText"

    // "extra" data keys
    const val KEY_CONTROLLER = "ControllerName"
    const val KEY_CONTROLLER_STATE = "ControllerState"
    const val KEY_SPOKEN_TEXT = "SpokenText"
}
