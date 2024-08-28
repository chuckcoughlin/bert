/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.util.Log
import chuckcoughlin.bertspeak.data.GeometryData
import chuckcoughlin.bertspeak.data.GeometryDataObserver
import chuckcoughlin.bertspeak.data.LogDataObserver
import chuckcoughlin.bertspeak.data.TextObserver
import com.google.mlkit.vision.face.Face

/**
 * Accept a new face structure from the FacesFragment, then handle all
 * communication from the robot concerning faaes.
 */
class FacesManager (service:DispatchService): CommunicationManager {
    private val dispatcher = service
    override val managerType = ManagerType.FACES
    override var managerState = ManagerState.OFF
    private var faceList: MutableList<String>    // Names of face owners
    private val faceObservers: MutableMap<String, TextObserver>

    override fun start() {}
    /**
     * Called when main activity is stopped. Clean up any resources.
     * To use again requires re-initialization.
     */
    override fun stop() {
    }

    fun reportFaceDetected(face:Face) {
        Log.i(CLSS, "Got a face")
    }
    /**
     * When a new log observer is registered, send a link to this manager.
     * The observer can then initialize its list, if desired. The manager
     * reference should be cleared on "unregisterSettingsObserver".
     * @param observer
     */
    fun register(observer: TextObserver) {
        faceObservers[observer.name] = observer
        observer.resetList(faceList)
    }

    fun unregister(observer: TextObserver) {
        for( key in faceObservers.keys ) {
            if( !observer.equals(faceObservers.get(key)) ) {
                faceObservers.remove(key,observer)
                break
            }
        }
    }

    private fun initializeObservers() {
        for (observer in faceObservers.values) {
            observer.resetList(faceList)
        }
    }

    /**
     * Notify observers regarding receipt of a new current face
     */
    private fun notifyObservers(index:Int) {
        Log.i(CLSS, String.format("notifyObservers: %d", index))
        for (observer in faceObservers.values) {
            observer.selectItem(index)
        }
    }

    private val CLSS = "FacesManager"

    /**
     * There should only be one text manager. owned by the dispatch service.
     * There are three queues:
     * 1) Spoken text, both requests and responses
     * 2) Logs
     * 3) Table (only the most recent)
     * When a subscriber first registers, the current queue to-date
     * is sent.
     */
    init {
        faceList     = mutableListOf<String>()
        faceObservers = mutableMapOf<String, TextObserver>()
    }
}
