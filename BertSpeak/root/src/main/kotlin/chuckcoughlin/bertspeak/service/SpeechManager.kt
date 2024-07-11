/**
 * Copyright 2023-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.core.content.getSystemService
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.common.NameValue
import chuckcoughlin.bertspeak.data.SettingsObserver
import chuckcoughlin.bertspeak.data.TextData
import chuckcoughlin.bertspeak.data.TextDataObserver
import chuckcoughlin.bertspeak.db.DatabaseManager
import java.util.Locale
import kotlin.math.roundToInt


/**
 * Handle the audible annunciation of results to the user. Text-to-speech.
 * The speech components must execute on the main thread
 * (and not in the service).
 */
class SpeechManager(service:DispatchService): CommunicationManager, SettingsObserver,
	                                          TextDataObserver,TextToSpeech.OnInitListener {
	override val managerType = ManagerType.SPEECH
	override var managerState = ManagerState.OFF
	val dispatcher: DispatchService
	private var annunciator: TextToSpeech
	override val name: String
	private val audioManager: AudioManager
	private var vol :Int   // Current volume as percent max

	override fun start() {
		Log.i(CLSS, String.format("start: "))
		annunciator.setOnUtteranceProgressListener(UtteranceListener())
		DatabaseManager.registerSettingsObserver(this)
		DispatchService.registerForTranscripts(this)
	}

	override fun stop() {
		annunciator.stop()
		annunciator.shutdown()
		DatabaseManager.unregisterSettingsObserver(this)
		DispatchService.unregisterForTranscripts(this)
		managerState = ManagerState.OFF
		dispatcher.reportManagerState(managerType,managerState)
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
			managerState = ManagerState.ACTIVE
		}
		else {
			Log.e(CLSS, String.format("onInit: TextToSpeech ERROR - %d", status))
			managerState = ManagerState.ERROR
		}
		dispatcher.reportManagerState(managerType,managerState)
	}
	// Annunciate the supplied text. Setting the volume in the bundle has no effect.
	// Turn off the audio when not in use.
	@Synchronized
	fun speak(txt:String) {
		Log.i(CLSS, String.format("SPEAK: %s (%s)", txt,managerState.name))
		if( managerState == ManagerState.ACTIVE) {
			//unMute()
			val bndl = Bundle()
			//bndl.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,vol.toFloat())
			annunciator.speak(txt,TextToSpeech.QUEUE_ADD,bndl,CLSS)
			//mute()
		}
	}


	/**
	 * Set the voice stream volume (does not update database)
	 * @param pcnt - volume percent of maximum (0-100)
	 */
	private fun setVolume(pcnt:Int) {
		val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
		vol = ((maxVol*pcnt)/100.0).roundToInt()
		Log.i(CLSS, String.format("setVolume %d (max=%d)", vol,maxVol))
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,vol,AudioManager.FLAG_PLAY_SOUND)
	}
	// Mute the beeps waiting for spoken input. At one point these methods were used to silence
	// annoying beeps with every onReadyForSpeech cycle. Currently they are not needed (??)
	fun mute() {
		Log.i(CLSS, String.format("mute"))
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
	}

	fun unMute() {
		Log.i(CLSS, String.format("unMute (vol = %d)", vol))
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_PLAY_SOUND)
	}

	// ===================== TextDataObserver =====================
	/**
	 * This is called when we first establish the observer.
	 * Annunciate the newest existing message
	 */
	override fun resetText(list: List<TextData>) {
		if(list.isNotEmpty()) {
			updateText(list.first())
		}
	}

	/**
	 * We only annunciate messages from the robot (ANS)
	 */
	override fun updateText(msg: TextData) {
		Log.i(name, String.format("updateText (%s):%s", msg.type, msg.message))
		if(msg.messageType == MessageType.ANS) {
			speak(msg.message)
		}
	}

	// ===================== SettingsObserver =====================
	override fun resetSettings(list: List<NameValue>) {
		for (ddata in list) {
			updateSetting(ddata)
		}
	}

	override fun updateSetting(data: NameValue) {
		if(data.name == BertConstants.BERT_VOLUME) {
			setVolume(data.value.toDouble().roundToInt())
		}
	}
	// ================= UtteranceProgressListener ========================
	// Use this to suppress feedback with analyzer while we're speaking
	inner class UtteranceListener: UtteranceProgressListener() {
		override fun onDone(utteranceId: String) {
		}

		override fun onError(utteranceId: String?, errorCode: Int) {
			Log.e(
				CLSS,
				String.format(
					"onError UtteranceListener ERROR - %s = %s (%d)",
					utteranceId,
					errorToText(errorCode),
					errorCode
				)
			)
		}

		private fun errorToText(err: Int): String {
			var error = when (err) {
				TextToSpeech.ERROR_INVALID_REQUEST -> "Invalid request"
				TextToSpeech.ERROR_NETWORK -> "Network error"
				TextToSpeech.ERROR_NETWORK_TIMEOUT -> "Network timeout"
				TextToSpeech.ERROR_SYNTHESIS -> "Failed to synthesize"
				else -> String.format("Error %d", err)
			}
			return error
		}

		@Deprecated("Deprecated in Java")
		override fun onError(err: String) {
			Log.e(CLSS, String.format("onError UtteranceListener ERROR - %s", err))
		}

		override fun onStart(utteranceId: String) {
		}
	}

	val CLSS = "SpeechManager"

	init {
		Log.i(CLSS,"init - initializing text to speech capability")
		dispatcher = service
		name = CLSS
		audioManager = dispatcher.context.getSystemService<AudioManager>() as AudioManager
		annunciator = TextToSpeech(dispatcher.context, this)
		// This is handled the same way in the CoverFragment startup
		try {
			vol = DatabaseManager.getSetting(BertConstants.BERT_VOLUME).toInt()
		}
		catch(nfe:NumberFormatException) {
			// Database entry was not a number
			Log.i(CLSS,"init - volume in database was not an integer")
			vol = 50
		}
	}
}
