/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.data.TextData
import chuckcoughlin.bertspeak.service.DispatchService.Companion.restoreAudio
import chuckcoughlin.bertspeak.service.DispatchService.Companion.suppressAudio
import chuckcoughlin.bertspeak.speech.Annunciator
import kotlinx.coroutines.DelicateCoroutinesApi
import java.util.Locale

/**
 * Handle the audible annunciation of results to the user.
 * It also contains
 *  * the speech components, since they must execute on the main thread
 *  * (and not in the service).
 */
class AnnunciationManager(service:DispatchService): CommunicationManager, TextToSpeech.OnInitListener {
	override val managerType = ManagerType.ANNUNCIATOR
	override var managerState = ManagerState.OFF
	val dispatcher: DispatchService
	var annunciator: Annunciator
	private val phrases: Array<String>

	override suspend fun run() {}
	override fun start() {
		Log.i(CLSS, String.format("start: "))
		annunciator.setOnUtteranceProgressListener(UtteranceListener())
	}

	override fun stop() {
		annunciator.stop()
	}

	/** @return a random startup phrase from the list. */
	private fun selectRandomText(): String {
		val rand = Math.random()
		val index = (rand * phrases.size).toInt()
		return phrases[index]
	}
	// ===================== OnInitListener ==========================
	override fun onInit(status: Int) {
		if(status == TextToSpeech.SUCCESS) {
			val voices: Set<Voice> = annunciator.voices
			for(v in voices) {
				if(v.name.equals("en-GB-SMTm00", ignoreCase = true)) {
					Log.i(CLSS, String.format("onInit: voice = %s %d",
						v.name, v.describeContents()))
					annunciator.voice = v
				}
			}
			annunciator.language = Locale.UK
			annunciator.setPitch(1.6f)      //Default＝1.0
			annunciator.setSpeechRate(1.0f) // Default=1.0
			Log.i(CLSS, "onInit: TextToSpeech initialized ...")
			val txt = selectRandomText()
			annunciator.speak(txt, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
			dispatcher.receiveSpokenText(txt)
		}
		else {
			Log.e(CLSS, String.format("onInit: TextToSpeech ERROR - %d", status))

		}
	}
	@Synchronized
	fun speak(msg: TextData) {
		if(msg.messageType == MessageType.ANS) {
			restoreAudio()
			annunciator.speak(msg.message)
			suppressAudio()
		}
	}

	// ================= UtteranceProgressListener ========================
	// Use this to suppress feedback with analyzer while we're speaking
	inner class UtteranceListener: UtteranceProgressListener() {
		@OptIn(DelicateCoroutinesApi::class)
		override fun onDone(utteranceId: String) {
			dispatcher.startSpeech()
		}

		override fun onError(utteranceId: String) {
			Log.e(CLSS, String.format("onError UtteranceListener ERROR - %s", utteranceId))
		}

		@OptIn(DelicateCoroutinesApi::class)
		override fun onStart(utteranceId: String) {
			dispatcher.stopSpeech()
		}
	}
	fun restoreAudio() {
		//AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		//audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
	}
	fun setVolume(vol:Int) {
		//AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		//audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
	}
	// Mute the beeps waiting for spoken input. At one point these methods were used to silence
	// annoying beeps with every onReadyForSpeech cycle. Currently they are not needed (??)
	fun suppressAudio() {
		//AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		//audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
	}

	val CLSS = "AnnunciationManager"
	val UTTERANCE_ID = CLSS

	init {
		Log.i(CLSS,"init - initializing annunciator")
		dispatcher = service
		annunciator = Annunciator(dispatcher.context, this)
		// Start phrases to choose from ...
		phrases = arrayOf(
			"My speech module is ready",
			"The speech connection is enabled",
			"I am ready for voice commands",
			"The speech controller is ready",
			"Marj I am ready",
			"Marj speak to me"
		)
	}
}
