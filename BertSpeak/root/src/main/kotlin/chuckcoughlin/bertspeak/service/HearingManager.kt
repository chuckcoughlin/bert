/**
 * Copyright 2023-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.getSystemService
import chuckcoughlin.bertspeak.service.ManagerState.ACTIVE


/**
 * Analyze spoken input to the application.Speech-to-text.
 * Note that the speech recognizer must run on the main UI thread.
 * This manager continuously listens for speech.
 * A notification facility is provided to avoid re-analyzing text
 * spoken by the robot.
 */
class HearingManager(service:DispatchService): CommunicationManager, RecognitionListener {
	override val managerType = ManagerType.HEARING
	override var managerState = ManagerState.OFF
	val dispatcher = service
	lateinit var sr: SpeechRecognizer
	private var initialized : Boolean
	private var suppress : Boolean
	private val audioManager: AudioManager

	private var recognizerIntent: Intent

	/** Must run on main thread. Start the recognizer */
	override fun start() {
		if (!SpeechRecognizer.isRecognitionAvailable(dispatcher.context)) {
			Log.w(CLSS, "start: ERROR (speech recognition is not supported on this device)")
			managerState = ManagerState.ERROR
			dispatcher.reportManagerState(ManagerType.HEARING, managerState)
		}
		else {
			sr = createRecognizer()
			initialized = true
			startListening()
		}
	}
	/**
	 * Must run on main thread. Shutdown the recognizer,
	 */
	override fun stop() {
		Log.i(CLSS, "Stop ...")
		stopListening()
		audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
	}
	/* There should be break a before we start listening to avoid feedback loop
	 * with spoken response. Note this will be cut short by
	 * a listener on the text-to-speech component.
	 */
	private fun startListening() {
		if(initialized) {
			Log.i(CLSS, "Start listening ...")
			sr.startListening(recognizerIntent)
			managerState = ACTIVE
			dispatcher.reportManagerState(ManagerType.HEARING,managerState)
		}
	}
	/* There is very little need for this. It should only
	 * be called when the manager shuts down.
	 */
	private fun stopListening() {
		Log.i(CLSS, "Stop listening ...")
		if( initialized ) {
			sr.destroy()
			managerState = ManagerState.OFF
			dispatcher.reportManagerState(ManagerType.HEARING,managerState)
		}
	}


	// ================ RecognitionListener ===============
	override fun onReadyForSpeech(params: Bundle) {
		Log.i(CLSS, "onReadyForSpeech")
		suppress = false
	}
	override fun onBeginningOfSpeech() {
		Log.i(CLSS, "onBeginningOfSpeech")
	}
	// Background level changed ...
	override fun onRmsChanged(rmsdB: Float) {}
	override fun onBufferReceived(buffer: ByteArray) {
		Log.i(CLSS, "onBufferReceived")
	}
	// Any results or error is reported after this ...
	override fun onEndOfSpeech() {
		Log.i(CLSS, "onEndOfSpeech")
	}
	override fun onError(error: Int) {
		var reason:String =
			when (error) {
				SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
				SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
					"Insufficient permission - Enable microphone in application"
				SpeechRecognizer.ERROR_NO_MATCH -> "no word match. Enunciate!"
				SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "speech timeout"
				SpeechRecognizer.ERROR_NETWORK -> "Network error"
				SpeechRecognizer.ERROR_CLIENT -> "client error"
				SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
					"recognition service is busy (started twice?)"
				SpeechRecognizer.ERROR_SERVER -> "server error"
				SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "too many requests"
				else -> String.format("Unrecognized error (%d) ", error)
			}

		// Some errors are benign, ignore
		if( error != SpeechRecognizer.ERROR_NO_MATCH ) {
			Log.e(CLSS, String.format("onError - %s", reason))  // Always log
			dispatcher.logError(managerType,reason)
			managerState = ManagerState.ERROR
			dispatcher.reportManagerState(ManagerType.HEARING, managerState)
		}
		suppress = false
		sr.startListening(recognizerIntent)
	}

	override fun onResults(results: Bundle) {
		Log.i(CLSS, "onResults \n$results")
		if( !results.isEmpty ) {
			// Fill the array with the strings the recognizer thought it could have heard, there should be 2
			val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
			for (i in matches.indices) {
				Log.i(CLSS, String.format("result %d: %s",i,matches[i]))
			}
			// The zeroth result is usually the space-separated one
			if( !matches.isEmpty() ) {
				var text = matches[0].lowercase()
				text = scrubText(text)

				if( !suppress ) {
					dispatcher.processSpokenText(text)
				}
				else {
					// We've just heard generated, not spoken, text
					Log.i(CLSS, String.format("SUPPRESSED result: %s",text))
				}
			}
		}
		suppress = false
		sr.startListening(recognizerIntent)
	}

	// We've configured the intent so all partials *should* be empty
	override fun onPartialResults(partialResults: Bundle) {}

	override fun onEvent(eventType: Int, params: Bundle) {}

	// We're tried the OnDevice recognizer with no luck.
	private fun createRecognizer() : SpeechRecognizer {
		val recognizer:SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(dispatcher.context)
		recognizer.setRecognitionListener(this)
		return recognizer
	}

	private fun createRecognizerIntent(): Intent {
		//val locale = "us-UK"
		val intent = Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE)
		intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false) // Partials are always empty
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
		//Give a hint to the recognizer about what the user is going to say
		intent.putExtra( RecognizerIntent.EXTRA_LANGUAGE_MODEL,
			             RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
		)
		//intent.putExtra(RecognizerIntent.EXTRA_MASK_OFFENSIVE_WORDS,true)
		intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,END_OF_PHRASE_TIME)
		// Max number of results. This is two attempts at deciphering, not a 2-word limit.
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 2)
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "chuckcoughlin.bertspeak.service");
		return intent
	}
	// Mute the beeps waiting for spoken input. At one point these methods were used to silence
	// annoying beeps with every onReadyForSpeech cycle.
	// In current state beeping is suppressed, and vizualizer works when volume increased
	fun mute() {
		Log.i(CLSS, String.format("mute"))
		audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
	}

	fun unMute() {
		//Log.i(CLSS, String.format("unMute (vol = %d)", vol))
		//audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_PLAY_SOUND)
	}
	/*
	 * Don't process the current speech we hear.
	 */
	fun suppressSpeech() {
		Log.i(CLSS, "suppressSpeech ...")
		suppress= true
	}

	fun toggleHearing() {
		if(managerState==ManagerState.ACTIVE) stopListening()
		else startListening()
	}
	/**
	 * Perform any cleanup of phrases that ease or correct the parsing.
	 * For example, replace ° with " degrees"
	 * @param text
	 * @return spiffy-clean text
	 */
	private fun scrubText(txt: String): String {
		var text = txt
		text = text.replace("birch", "bert")
		text = text.replace("°", " degrees")
		text = text.replace("exposition", "x position")
		text = text.replace("fries", "freeze")
		text = text.replace("too", "2")
		text = text.replace("what's", "what is")
		text = text.replace("why", "y")
		text = text.replace("zero", "0")
		return text
	}

	val CLSS = "HearingManager"
	val END_OF_PHRASE_TIME = 150 // Silence to indicate end-of-input

	/**
	 * Creating the speech recognizer must be done on the main thread..
	 * Muting the audio manager stops the annoying beep of the SpeechRecognizer
	 */
	init {
		audioManager = dispatcher.context.getSystemService<AudioManager>() as AudioManager
		mute()  // try to stop the beeping
		recognizerIntent = createRecognizerIntent()
		initialized = false
		suppress = false
	}
}
