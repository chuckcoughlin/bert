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
import chuckcoughlin.bertspeak.common.ConfigurationConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.common.NameValue
import chuckcoughlin.bertspeak.data.SettingsObserver
import chuckcoughlin.bertspeak.data.LogData
import chuckcoughlin.bertspeak.data.LogDataObserver
import chuckcoughlin.bertspeak.db.DatabaseManager
import java.util.Locale
import kotlin.math.roundToInt


/**
 * Handle the audible annunciation of results to the user. Text-to-speech.
 * The speech components must execute on the main thread
 * (and not in the service).
 */
class SpeechManager(service:DispatchService): CommunicationManager, SettingsObserver,
	                                          LogDataObserver,TextToSpeech.OnInitListener {
	override val managerType = ManagerType.SPEECH
	override var managerState = ManagerState.OFF
	val dispatcher: DispatchService
	private var textToSpeech: TextToSpeech
	override val name: String
	private val audioManager: AudioManager
	private var vol :Int   // Current volume as percent max

	override fun start() {
		Log.i(CLSS, String.format("start: "))
		textToSpeech.setOnUtteranceProgressListener(UtteranceListener())
		DatabaseManager.registerSettingsObserver(this)
		DispatchService.registerForTranscripts(this)
	}

	override fun stop() {
		textToSpeech.stop()
		textToSpeech.shutdown()
		DatabaseManager.unregisterSettingsObserver(this)
		DispatchService.unregisterForTranscripts(this)
		managerState = ManagerState.OFF
		dispatcher.reportManagerState(managerType,managerState)
	}

	// ===================== OnInitListener ==========================
	override fun onInit(status: Int) {
		if(status == TextToSpeech.SUCCESS) {
			val voices: Set<Voice> = textToSpeech.voices
			for( v in voices) {
				// Log the availble English names
				if( name.startsWith("en-") ) {
					Log.i(CLSS,String.format("onInit: voice = %s %d", v.name, v.describeContents()))
					// en-GB-SMTm00 - on tablet female
					// en-GB-SMTg02
					// en-GB-SMTl02
					// en-gb-x-gbd-network
					if( v.name.startsWith("en-GB-SMTl02", ignoreCase = true) ) {
						Log.i(CLSS, String.format("onInit: voice = %s %d",v.name, v.describeContents()))
						val result = textToSpeech.setVoice(v)
						Log.i(CLSS, String.format("onInit: set voice (%s)",
							if (result == TextToSpeech.SUCCESS) "SUCCESS" else "ERROR"))
					}
				}
			}
			textToSpeech.language = Locale.UK
			textToSpeech.setPitch(0.6f)      //Default＝1.0
			textToSpeech.setSpeechRate(1.0f) // Default=1.0
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
	// The caller must tell the hearing manager to mark the end of speech
	@Synchronized
	fun speak(txt:String) {
		Log.i(CLSS, String.format("SPEAK: %s (%s)", txt,managerState.name))
		if( managerState == ManagerState.ACTIVE) {
			val bndl = Bundle()
			//bndl.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,vol.toFloat())
			textToSpeech.speak(txt,TextToSpeech.QUEUE_FLUSH,bndl,CLSS)
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

	// ===================== TextDataObserver =====================
	/**
	 * This is called when we first establish the observer.
	 * Annunciate the newest existing message
	 */
	override fun resetText(list: List<LogData>) {
		if(list.isNotEmpty()) {
			updateText(list.first())
		}
	}

	/**
	 * Annunciate messages from the robot (ANS).
	 * Turn off listening to avoid double-analyzing.
	 */
	override fun updateText(msg: LogData) {
		Log.i(name, String.format("updateText (%s):%s", msg.type, msg.message))
		if(msg.messageType == MessageType.ANS) {
			dispatcher.suppressSpeech()
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
		if(data.name == ConfigurationConstants.BERT_VOLUME) {
			setVolume(data.value.toDouble().roundToInt())
		}
	}
	// ================= UtteranceProgressListener ========================
	// Use this to suppress feedback with analyzer while we're speaking
	inner class UtteranceListener: UtteranceProgressListener() {
		override fun onDone(utteranceId: String) {
		}

		override fun onError(utteranceId: String?, errorCode: Int) {
			Log.e(CLSS,String.format("onError UtteranceListener ERROR - %s = %s (%d)",
					utteranceId,errorToText(errorCode),errorCode))
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

		@Deprecated("Deprecated, not removed")
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
		textToSpeech = TextToSpeech(dispatcher.context, this)
		// This is handled the same way in the CoverFragment startup
		try {
			vol = DatabaseManager.getSetting(ConfigurationConstants.BERT_VOLUME).toInt()
		}
		catch(nfe:NumberFormatException) {
			// Database entry was not a number
			Log.i(CLSS,"init - volume in database was not an integer")
			vol = 50
		}
	}
}
