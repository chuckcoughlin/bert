/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import chuckcoughlin.bertspeak.service.BluetoothHandler
import chuckcoughlin.bertspeak.service.FacilityState
import chuckcoughlin.bertspeak.service.TieredFacility
import java.util.*

/**
 * This class analyzes speech, converting it into text (lists of words).
 * The SpeechRecognizer methods must be executed on the main application thread (UI thread).
 */
class SpeechAnalyzer(h: BluetoothHandler, c: Context) : RecognitionListener {
    private val context: Context
    private var listening = false
    private var sr: SpeechRecognizer? = null
    private val handler: BluetoothHandler
    private var recognizerIntent: Intent? = null

    /**
     * Stop any recognition in progress.
     * Must be called from UI thread
     */
    fun cancel() {
        if (sr != null) {
            sr!!.cancel()
        }
        listening = false
    }

    /**
     * If we are not currently listening, then start.
     * Must be called from UI thread.
     */
    fun listen() {
        if (!listening) {
            startListening()
        }
    }

    fun start() {
        recognizerIntent = createRecognizerIntent()
        resetSpeechRecognizer()
        startListening()
    }

    fun shutdown() {
        if (sr != null) {
            sr!!.stopListening()
            try {
                sr!!.destroy()
            } catch (iae: IllegalArgumentException) {
                Log.i(CLSS, String.format("%s:shutdown: (%s)", CLSS, iae.localizedMessage))
            } // Happens in emulator
        }
        sr = null
    }

    // Delay before we start listening to avoid feedback loop
    // with spoken response. Note this will be cut short by
    // a listener on the text-to-speech component.
    private fun startListening() {
        if (sr != null) {
            Log.i(CLSS, "Start listening ...")
            listening = true
            sr!!.startListening(recognizerIntent)
        }
    }

    private fun resetSpeechRecognizer() {
        if (sr != null) {
            sr!!.destroy()
        }
        sr = SpeechRecognizer.createSpeechRecognizer(context)
        sr!!.setRecognitionListener(this)
        listening = false
    }

    // ========================================= RecognitionListener ============================
    override fun onReadyForSpeech(params: Bundle) {
        Log.i(CLSS, "onReadyForSpeech");
    }

    override fun onBeginningOfSpeech() {
        //Log.i(CLSS, "onBeginningOfSpeech");
        handler!!.reportConnectionState(TieredFacility.VOICE, FacilityState.ACTIVE)
    }

    // Background level changed ...
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray) {
        Log.i(CLSS, "onBufferReceived")
    }

    override fun onEndOfSpeech() {
        Log.i(CLSS, "onEndofSpeech");
    }

    override fun onError(error: Int) {
        var reason: String? = null
        when (error) {
            SpeechRecognizer.ERROR_AUDIO -> reason = String.format("Audio recording error")
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> reason =
                String.format("INSUFFICIENT PERMISSION - Enable microphone in application")
            SpeechRecognizer.ERROR_NO_MATCH -> Log.i(
                CLSS,
                String.format("SpeechRecognition: Error - no word match. Enunciate!")
            )
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Log.d(
                CLSS,
                String.format("SpeechRecognition: Error - speech timeout")
            )
            SpeechRecognizer.ERROR_NETWORK -> reason = String.format("Network error")
            SpeechRecognizer.ERROR_CLIENT -> reason = String.format("Error - in client")
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Log.i(
                CLSS,
                String.format("Error - recognition service is busy (started twice?)")
            )
            SpeechRecognizer.ERROR_SERVER -> reason = String.format("Error - in server")
            else -> reason = String.format("ERROR (%d) ", error)
        }
        if (reason != null) {
            Log.e(CLSS, String.format("SpeechRecognizer: Error - %s", reason))
            handler.handleVoiceError(reason)
        }
        // Try again
        resetSpeechRecognizer()
        startListening()
    }

    override fun onResults(results: Bundle) {
        Log.i(CLSS, "onResults \n$results")
        // Fill the list view with the strings the recognizer thought it could have heard, there should be 5, based on the call
        val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
        for (i in matches.indices) {
            Log.i(CLSS, "result " + matches[i])
        }
        var text = matches[0]
        text = scrubText(text)
        handler!!.receiveSpokenText(text)
        handler.reportConnectionState(TieredFacility.VOICE, FacilityState.WAITING)
        startListening() // Repeat forever
    }

    override fun onPartialResults(partialResults: Bundle) {
        Log.i(CLSS, "onPartialResults")
    }

    override fun onEvent(eventType: Int, params: Bundle) {
        Log.i(CLSS, "onEvent $eventType")
    }

    private fun createRecognizerIntent(): Intent {
        val locale = "us-UK"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, javaClass.getPackage().name)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false) // Partials are always empty
        intent.putExtra(
            RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
            END_OF_PHRASE_TIME
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en")
        //Give a hint to the recognizer about what the user is going to say
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        // Max number of results. This is three attempts at deciphering, not a 3-word limit.
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 2)
        return intent
    }

    /**
     * Perform any cleanup of phrases that ease or correct the parsing.
     * For example, replace ° with " degrees"
     * @param text
     * @return spiffy-clean text
     */
    private fun scrubText(text: String): String {
        var text = text
        text = text.replace("°", " degrees").lowercase(Locale.getDefault())
        text = text.replace("exposition", "x position")
        text = text.replace("fries", "freeze")
        text = text.replace("zero", "0")
        return text
    }

    companion object {
        private const val CLSS = "SpeechAnalyzer"
        private const val END_OF_PHRASE_TIME = 2000 // Silence to indicate end-of-input
    }

    init {
        listening = false
        context = c
        handler = h
    }
}
