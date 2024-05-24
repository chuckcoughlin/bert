/**
 * Copyright 2023-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.core.content.getSystemService
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.common.NameValue
import chuckcoughlin.bertspeak.data.TextData
import chuckcoughlin.bertspeak.db.DatabaseManager
import chuckcoughlin.bertspeak.speech.Annunciator
import kotlinx.coroutines.DelicateCoroutinesApi
import java.lang.NumberFormatException
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Handle the audible annunciation of results to the user. Text-to-speech.
 * The speech components must execute on the main thread
 * (and not in the service).
 */
class SpeechManager(service:DispatchService): CommunicationManager, TextToSpeech.OnInitListener {
	override val managerType = ManagerType.SPEECH
	override var managerState = ManagerState.OFF
	val dispatcher: DispatchService
	var annunciator: Annunciator
	private val audio: AudioManager
	private var vol :Int   // Current volume

	override fun start() {
		Log.i(CLSS, String.format("start: "))
		annunciator.setOnUtteranceProgressListener(UtteranceListener())
	}

	override fun stop() {
		annunciator.stop()
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
	// Annunciate the supplied text.
	// Turn off the audio when not in use.
	@Synchronized
	fun speak(msg: TextData) {
		if( managerState.equals(ManagerState.ACTIVE)) {
			if(msg.messageType == MessageType.ANS) {
				restoreAudio(vol)
				annunciator.speak(msg.message)
				suppressAudio()
			}
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
		val pcnt = DatabaseManager.getSetting(BertConstants.BERT_VOLUME).toDouble()
		val maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
		val vol = ((maxVol*pcnt)/100.0).roundToInt()
		restoreAudio(vol)
	}
	private fun restoreAudio(v:Int) {
		audio.setStreamVolume(AudioManager.STREAM_VOICE_CALL, v, 0);
	}
	/**
	 * Set the voice stream volume.
	 * @param pcnt - volume percent of maximum (0-100)
	 */
	fun setVolume(pcnt:Int) {
		Log.i(CLSS, String.format("setVolume %d", pcnt))
		val maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
		val vol = ((maxVol*pcnt)/100.0).roundToInt()
		val nv = NameValue(BertConstants.BERT_VOLUME,pcnt.toString())
		DatabaseManager.updateSetting(nv)
		audio.setStreamVolume(AudioManager.STREAM_VOICE_CALL,vol,AudioManager.FLAG_PLAY_SOUND)
	}
	// Mute the beeps waiting for spoken input. At one point these methods were used to silence
	// annoying beeps with every onReadyForSpeech cycle. Currently they are not needed (??)
	fun suppressAudio() {
		audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
	}

	val CLSS = "SpeechManager"
	val UTTERANCE_ID = CLSS

	init {
		Log.i(CLSS,"init - initializing annunciator")
		dispatcher = service
		audio = dispatcher.context.getSystemService<AudioManager>() as AudioManager
		annunciator = Annunciator(dispatcher.context, this)
		// This is handled the same way in the CoverFragment startup
		try {
			vol = DatabaseManager.getSetting(BertConstants.BERT_VOLUME).toInt()
			setVolume(vol)
		}
		catch(nfe:NumberFormatException) {
			// Database entry was not a number
			Log.i(CLSS,"init - volume in database was not an integer")
			vol = 50
		}
	}
}
