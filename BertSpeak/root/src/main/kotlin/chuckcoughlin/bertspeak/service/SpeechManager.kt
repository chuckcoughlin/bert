/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Analyze spoken input to the application.
 * Note that the speech recognizer must run on the main UI thread.
 * This manager gets started nad stopped with the need for speech recognition.
 */
class SpeechManager(service:DispatchService): CommunicationManager, RecognitionListener {
	override val managerType = ManagerType.SPEECH
	override var managerState = ManagerState.OFF
	val dispatcher = service
	private var listening = false
	private var sr: SpeechRecognizer
	private var recognizerIntent: Intent

	override suspend fun run() {}
	/** Must run on main thread */
	override fun start() {
		Log.i(CLSS, "Start ...")
		resetSpeechRecognizer()
		if(!listening) {
			startListening()
		}
		dispatcher.reportManagerState(ManagerType.SPEECH,ManagerState.ACTIVE)
	}


	/**
	 * Must run on main thread
	 */
	override fun stop() {
		Log.i(CLSS, "Stop ...")
		sr.stopListening()
		try {
			sr.destroy()
		}
		catch(iae: IllegalArgumentException) {
			Log.w(CLSS, String.format("%s:stop: (%s)", CLSS, iae.localizedMessage))
		} // Happens in emulator
		dispatcher.reportManagerState(ManagerType.SPEECH, ManagerState.OFF)
	}

	// Delay before we start listening to avoid feedback loop
	// with spoken response. Note this will be cut short by
	// a listener on the text-to-speech component.
	private fun startListening() {
		Log.i(CLSS, "Start listening ...")
		listening = true
		sr.startListening(recognizerIntent)
	}

	private fun resetSpeechRecognizer() {
		Log.w(CLSS, String.format("resetSpeechRecognizer:", CLSS))
		try {
			sr.destroy()
		}
		catch(ex:Exception) {
			Log.w(CLSS, String.format("resetSpeechRecognizer: destroying (%s)", CLSS, ex.localizedMessage))
		}
		sr = SpeechRecognizer.createSpeechRecognizer(DispatchService.instance.context)
		sr.setRecognitionListener(this)
		listening = false
	}


	// ================ RecognitionListener ===============
	override fun onReadyForSpeech(params: Bundle) {
		Log.i(CLSS, "onReadyForSpeech")
	}

	override fun onBeginningOfSpeech() {
		Log.i(CLSS, "onBeginningOfSpeech");
		dispatcher.reportManagerState(ManagerType.SPEECH, ManagerState.ACTIVE)
	}

	// Background level changed ...
	override fun onRmsChanged(rmsdB: Float) {}
	override fun onBufferReceived(buffer: ByteArray) {
		Log.i(CLSS, "onBufferReceived")
	}

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
			else -> String.format("ERROR (%d) ", error)
		}
		Log.e(CLSS, String.format("onError - %s", reason))
		dispatcher.logError(managerType,reason)

		// Try again
		resetSpeechRecognizer()
		startListening()
	}


	override fun onResults(results: Bundle) {
		if( !results.isEmpty ) {
			Log.i(CLSS, "onResults \n$results")
			// Fill the list view with the strings the recognizer thought it could have heard, there should be 5, based on the call
			val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
			for (i in matches.indices) {
				Log.i(CLSS, "result " + matches[i])
			}
			var text = matches[0]
			text = scrubText(text)
			dispatcher.receiveSpokenText(text)
			dispatcher.reportManagerState(managerType, ManagerState.PENDING)
			startListening() // Repeat forever
		}
	}

	override fun onPartialResults(partialResults: Bundle) {
		Log.i(CLSS, "onPartialResults")
	}

	override fun onEvent(eventType: Int, params: Bundle) {
		Log.i(CLSS, "onEvent $eventType")
	}

	private fun createRecognizerIntent(): Intent {
		//val locale = "us-UK"
		val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, javaClass.getPackage()?.name)
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
	private fun scrubText(txt: String): String {
		var text = txt
		text = text.replace("°", " degrees").lowercase(Locale.getDefault())
		text = text.replace("exposition", "x position")
		text = text.replace("fries", "freeze")
		text = text.replace("zero", "0")
		return text
	}

	val CLSS = "SpeechManager"
	val END_OF_PHRASE_TIME = 2000 // Silence to indicate end-of-input

	init {
		managerState = ManagerState.OFF
		Log.i(CLSS, "init prior")
		sr = SpeechRecognizer.createSpeechRecognizer(dispatcher.context)
		Log.i(CLSS, "init post")
		recognizerIntent = createRecognizerIntent()
		listening = false
	}
}