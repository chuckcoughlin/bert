/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.speech

import android.speech.tts.TextToSpeech
import android.content.*
import android.util.Log

/**
 * Pronounce a supplied phrase.
 */
class Annunciator
/**
 * The Android facility for pronouncing text. Configuration
 * methods are not effective in the constructor
 */
    (context: Context?, listener: OnInitListener?) : TextToSpeech(context, listener) {
    /**
     * Convert error text to speach in the chosen language. We are not yet using the distance.
     * @param text the text to pronounce
     */
    fun speak(text: String?) {
        Log.i(CLSS, String.format("speak: %s", text))
        var result = 0
        result = speak(text, QUEUE_FLUSH, null, UTTERANCE_ID)
        if (result != SUCCESS) {
            Log.w(CLSS, String.format("speak: %s pronouncing: %s", errorToText(result), text))
        }
    }

    private fun errorToText(err: Int): String {
        var error = "??"
        error = when (err) {
            ERROR_INVALID_REQUEST -> "Invalid request"
            ERROR_NETWORK -> "Network error"
            ERROR_NETWORK_TIMEOUT -> "Network timeout"
            ERROR_SYNTHESIS -> "Failed to synthesize"
            else -> String.format("Error %d", err)
        }
        return error
    }

    companion object {
        private const val CLSS = "Annunciator"
        private const val UTTERANCE_ID = CLSS
    }
}