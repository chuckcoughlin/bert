/**
 * Copyright 2023-2024 Charles Coughlin. All rights reserved.
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
import java.util.Locale

/**
 * Analyze spoken input to the application. Text-to-speech.
 * Note that the speech recognizer must run on the main UI thread.
 * This manager gets started and stopped with the need for speech recognition.
 * There can be feedback between the recognizer and speech-to-text.
 * They should not run at the same time.
 */
class HearingManager(service:DispatchService): CommunicationManager, RecognitionListener {
	override val managerType = ManagerType.HEARING
	override var managerState = ManagerState.OFF
	val dispatcher = service
	private val audioManager: AudioManager
	private var sr: SpeechRecognizer?
	private var recognizerIntent: Intent

	/** Must run on main thread. Start the recognizer */
	override fun start() {
		if (!SpeechRecognizer.isRecognitionAvailable(dispatcher.context)) {
			Log.w(CLSS, "start: ERROR (speech recognition is not supported on this device)")
			dispatcher.reportManagerState(ManagerType.HEARING, ManagerState.ERROR)
		}
		else {
			Log.i(CLSS, String.format("start: "))
			startListening()
		}
	}
	/**
	 * Must run on main thread. Shutdown the recognizer,
	 */
	override fun stop() {
		Log.i(CLSS, "Stop ...")
		stopListening()
	}

	/* There should be break a before we start listening to avoid feedback loop
	 * with spoken response. Note this will be cut short by
	 * a listener on the text-to-speech component.
	 */
	fun startListening() {
		if(sr == null && !managerState.equals(ManagerState.ERROR)) {
			Log.i(CLSS, "Start listening ...")
			sr = createRecognizer()
			sr!!.startListening(recognizerIntent)
		}
		managerState = ACTIVE
		dispatcher.reportManagerState(ManagerType.HEARING,managerState)
	}
	/* There is very little need for this. It should only
	 * be called if speech is in progress
	 */
	fun stopListening() {
		if( sr!=null ) {
			Log.i(CLSS, "Stop listening ...")
			sr!!.stopListening()
			try {
				sr!!.destroy()
			}
			catch (iae: IllegalArgumentException) {
				// Happens in emulator
				Log.e(CLSS, String.format("stop: destroy (%s))", iae.localizedMessage))
			}
		}
		sr = null
		managerState = ManagerState.OFF
		dispatcher.reportManagerState(ManagerType.HEARING,managerState)
	}


	// ================ RecognitionListener ===============
	override fun onReadyForSpeech(params: Bundle) {
		Log.i(CLSS, "onReadyForSpeech")
	}
	override fun onBeginningOfSpeech() {
		//Log.i(CLSS, "onBeginningOfSpeech");
	}
	// Background level changed ...
	override fun onRmsChanged(rmsdB: Float) {}
	override fun onBufferReceived(buffer: ByteArray) {
		Log.i(CLSS, "onBufferReceived")
	}
	// Any error is reported after this ...
	override fun onEndOfSpeech() {
		Log.i(CLSS, "onEndofSpeech")
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
		Log.e(CLSS, String.format("onError - %s", reason))  // Always log
		dispatcher.logError(managerType,reason)

		// Some errors are benign, ignore
		if( error != SpeechRecognizer.ERROR_NO_MATCH ) {
			managerState = ManagerState.ERROR
			dispatcher.reportManagerState(ManagerType.HEARING, managerState)
		}
	}

	override fun onResults(results: Bundle) {
		Log.i(CLSS, "onResults \n$results")
		if( !results.isEmpty ) {
			// Fill the array with the strings the recognizer thought it could have heard, there should be 5
			val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
			for (i in matches.indices) {
				Log.i(CLSS, String.format("result %d: %s",i,matches[i]))
			}
			// The zeroth result is usually the space-separated one
			if( !matches.isEmpty()) {
				var text = matches[0]
				text = scrubText(text)
				dispatcher.processSpokenText(text)
			}
		}
	}

	/**
	 * We've configured the intent so all partials *should* be empty
	 */
	override fun onPartialResults(partialResults: Bundle) {
		Log.i(CLSS, "onPartialResults")
	}

	override fun onEvent(eventType: Int, params: Bundle) {}

	private fun createRecognizer() : SpeechRecognizer {
		Log.i(CLSS, String.format("createRecognizer: "))
		val recognizer:SpeechRecognizer

		if( SpeechRecognizer.isOnDeviceRecognitionAvailable(dispatcher.context) ) {
			Log.i(CLSS, "start: speechRecognizer is on-device")
			recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(dispatcher.context)
		}
		else  {
			Log.i(CLSS, "start: speechRecognizer is off-device")
			recognizer = SpeechRecognizer.createSpeechRecognizer(dispatcher.context)
		}
		recognizer.setRecognitionListener(this)

		return recognizer
	}
	private fun createRecognizerIntent(): Intent {
		//val locale = "us-UK"
		val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
		intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false) // Partials are always empty
		/* intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,SPEECH_MIN_TIME)
		intent.putExtra(
			RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,END_OF_PHRASE_TIME
		)
		*/
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
		//Give a hint to the recognizer about what the user is going to say
		intent.putExtra(
			RecognizerIntent.EXTRA_LANGUAGE_MODEL,
			RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
		)
		// Max number of results. This is two attempts at deciphering, not a 2-word limit.
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 2)
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "chuckcoughlin.bertspeak.service");
		return intent
	}
	// Mute the beeps waiting for spoken input. At one point these methods were used to silence
	// annoying beeps with every onReadyForSpeech cycle. Currently they are not needed (??)
	fun mute() {
		Log.i(CLSS, String.format("mute"))
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
	}

	fun unMute() {
		//Log.i(CLSS, String.format("unMute (vol = %d)", vol))
		//audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_PLAY_SOUND)
	}
	/**
	 * Perform any cleanup of phrases that ease or correct the parsing.
	 * For example, replace ° with " degrees"
	 * @param text
	 * @return spiffy-clean text
	 */
	private fun scrubText(txt: String): String {
		var text = txt
		text = text.replace("°", " degrees").lowercase(Locale.getDefault())
		text = text.replace("exposition", "x position")
		text = text.replace("fries", "freeze")
		text = text.replace("zero", "0")
		return text
	}

	val CLSS = "HearingManager"
	val DELAY_TIME = 1000L
	val SPEECH_MIN_TIME = 100     // Word must be at least this long
	val END_OF_PHRASE_TIME = 2000 // Silence to indicate end-of-input

	/**
	 * Creating the speech recognizer must be done on the main thread.
	 */
	init {
		audioManager = dispatcher.context.getSystemService<AudioManager>() as AudioManager
		recognizerIntent = createRecognizerIntent()
		sr = null
	}
}
