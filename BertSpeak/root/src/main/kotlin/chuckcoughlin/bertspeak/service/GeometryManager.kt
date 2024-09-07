/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.util.Log
import chuckcoughlin.bertspeak.data.GeometryData
import chuckcoughlin.bertspeak.data.JsonData
import chuckcoughlin.bertspeak.data.GeometryDataObserver

/**
 * The geometry manager receives positional information from the robot
 * and converts it for display by the animation fragment.
 */
class GeometryManager (service:DispatchService): CommunicationManager {
    private val dispatcher = service
    override val managerType = ManagerType.GEOMETRY
    override var managerState = ManagerState.OFF
    private var geometry : GeometryData  // Current geometry
    private val geometryObservers: MutableMap<String, GeometryDataObserver>

    override fun start() {}
    /**
     * Called when main activity is stopped. Clean up any resources.
     * To use again requires re-initialization.
     */
    override fun stop() {
    }

    /**
     * When a new log observer is registered, send a link to this manager.
     * The observer can then initialize its list, if desired. The manager
     * reference should be cleared on "unregisterSettingsObserver".
     * @param observer
     */
    fun register(observer: GeometryDataObserver) {
        geometryObservers[observer.name] = observer
        observer.resetGeometry(geometry)
    }

    fun unregister(observer: GeometryDataObserver) {
        for( key in geometryObservers.keys ) {
            if( !observer.equals(geometryObservers.get(key)) ) {
                geometryObservers.remove(key,observer)
                break
            }
        }
    }

    private fun initializeObservers() {
        for (observer in geometryObservers.values) {
            observer.resetGeometry(geometry)
        }
    }

    /**
     * Notify log observers regarding receipt of a new message.
     */
    private fun notifyObservers(geom: GeometryData) {
        for (observer in geometryObservers.values) {
            observer.updateGeometry(geom)
        }
    }

    private val CLSS = "GeometryManager"

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
        geometry                 =  GeometryData("")
        geometryObservers        = mutableMapOf<String, GeometryDataObserver>()
    }
}
