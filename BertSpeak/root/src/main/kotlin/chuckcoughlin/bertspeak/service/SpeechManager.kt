/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import chuckcoughlin.bertspeak.speech.SpeechAnalyzer

/**
 * Analyze spoken input to the application.
 * It also contains the speech components, since they must execute on the main thread
 * (and not in the service).
 */
class SpeechManager(service:DispatchService): CommunicationManager {
	override val type = ManagerType.SPEECH
	override var state = ManagerState.OFF
	val dispatcher = service
	lateinit var analyzer: SpeechAnalyzer

	override fun start() {
		activateSpeechAnalyzer()
	}

	override fun stop() {
		deactivateSpeechAnalyzer()
	}

	// Turn off the audio to mute the annoying beeping
	fun activateSpeechAnalyzer() {
		DispatchService.suppressAudio()
		analyzer = SpeechAnalyzer(dispatcher)
		analyzer.start()
		analyzer.listen()
		dispatcher.reportManagerStatus(ManagerType.SPEECH,ManagerState.ACTIVE)
	}

	fun deactivateSpeechAnalyzer() {
		DispatchService.restoreAudio()
		analyzer.shutdown()
		dispatcher.reportManagerStatus(ManagerType.SPEECH,ManagerState.OFF)
	}
	val CLSS = "AnnunciationController"

	init {
		state = ManagerState.OFF
	}
}