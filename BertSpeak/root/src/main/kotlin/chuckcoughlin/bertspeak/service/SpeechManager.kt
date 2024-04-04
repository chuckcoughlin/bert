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
 * This manager gets started and stopped with the need for speech recognition.
 */
class SpeechManager(service:DispatchService): CommunicationManager, RecognitionListener {
	override val managerType = ManagerType.SPEECH
	override var managerState = ManagerState.OFF
	val dispatcher = service
	var listening = false
	private var sr: SpeechRecognizer
	private var recognizerIntent: Intent

	override suspend fun run() {}
	/** Must run on main thread */
	override fun start() {
		Log.i(CLSS, "Start ...")
		if( !SpeechRecognizer.isRecognitionAvailable(dispatcher.context) ) {
			Log.w(CLSS, "start: ERROR (speech recognition is not supported on this device)")
			dispatcher.reportManagerState(ManagerType.SPEECH,ManagerState.ERROR)
		}
		else {
			resetSpeechRecognizer()
			startListening()
			dispatcher.reportManagerState(ManagerType.SPEECH,ManagerState.ACTIVE)
		}
	}

	/**
	 * Must run on main thread
	 */
	override fun stop() {
		Log.i(CLSS, "Stop ...")
		if( listening ) {
			sr.stopListening()
			try {
				sr.destroy()
			}
			catch(iae:IllegalArgumentException) {
				// Happens in emulator
				Log.e(CLSS,String.format("stop: destroy (%s))",iae.localizedMessage))
			}
			listening = false
		}
		dispatcher.reportManagerState(ManagerType.SPEECH, ManagerState.OFF)
	}

	// Delay before we start listening to avoid feedback loop
	// with spoken response. Note this will be cut short by
	// a listener on the text-to-speech component.
	@Synchronized
	private fun startListening() {
		Log.i(CLSS, "Start listening ...")
		if( !listening ) {
			sr.setRecognitionListener(this)
			sr.startListening(recognizerIntent)
			listening = true
			dispatcher.reportManagerState(ManagerType.SPEECH, ManagerState.ACTIVE)
		}
	}

	@Synchronized
	private fun resetSpeechRecognizer() {
		Log.i(CLSS, "reset speech recognizer ...")
		if( listening ) {
			try {
				sr.destroy()
			}
			catch(iae:IllegalArgumentException) {
				// Happens in emulator
				Log.e(CLSS,String.format("resetSpeechRecognizer: destroy (%s))",iae.localizedMessage))
			}
			listening = false
			dispatcher.reportManagerState(ManagerType.SPEECH, ManagerState.OFF)
		}
		sr = SpeechRecognizer.createSpeechRecognizer(DispatchService.instance.context)
	}
	// ================ RecognitionListener ===============
	override fun onReadyForSpeech(params: Bundle) {
		Log.i(CLSS, "onReadyForSpeech")
	}

	override fun onBeginningOfSpeech() {
		Log.i(CLSS, "onBeginningOfSpeech");
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
		Log.i(CLSS, "onResults \n$results")
		if( !results.isEmpty ) {
			// Fill the list view with the strings the recognizer thought it could have heard, there should be 5, based on the call
			val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
			for (i in matches.indices) {
				Log.i(CLSS, "result " + matches[i])
			}
			if( !matches.isEmpty()) {
				var text = matches[0]
				text = scrubText(text)
				dispatcher.receiveSpokenText(text)
				dispatcher.reportManagerState(managerType, ManagerState.PENDING)
			}
			startListening() // Repeat forever
		}
	}

	override fun onPartialResults(partialResults: Bundle) {
		Log.i(CLSS, "onPartialResults")
		if( !partialResults.isEmpty ) {
			Log.i(CLSS, "partial result = " + partialResults)
			// Fill the list view with the strings the recognizer thought it could have heard, there should be 5, based on the call
			val matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
			for (i in matches.indices) {
				Log.i(CLSS, "partial result " + matches[i])
			}
			if( !matches.isEmpty() ) {
				var text = matches[0]
				text = scrubText(text)
				dispatcher.receiveSpokenText(text)
				dispatcher.reportManagerState(managerType, ManagerState.PENDING)
			}
			startListening() // Repeat forever
		}
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
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
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
	val DELAY_TIME = 1000L
	val END_OF_PHRASE_TIME = 2000 // Silence to indicate end-of-input


	init {
		managerState = ManagerState.OFF
		recognizerIntent = createRecognizerIntent()
		sr = SpeechRecognizer.createSpeechRecognizer(DispatchService.instance.context)
	}
}
