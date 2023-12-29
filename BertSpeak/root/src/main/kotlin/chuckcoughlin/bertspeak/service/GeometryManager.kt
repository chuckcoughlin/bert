package chuckcoughlin.bertspeak.service

import android.util.Log
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.FixedSizeList
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.data.GeometryData
import chuckcoughlin.bertspeak.data.GeometryDataObserver
import chuckcoughlin.bertspeak.data.TextData
import chuckcoughlin.bertspeak.data.TextDataObserver

/**
 * The geometry manager is a repository of the positional status of joints.
 */
class GeometryManager (service:DispatchService): CommunicationManager {
    override val type = ManagerType.GEOMETRY
    override var state = ManagerState.OFF
    private val jointList : MutableList<GeometryData>
    private val geometryObservers: MutableMap<String, GeometryDataObserver>

    /**
     *
     */
    override fun start() {
    }
    /**
     * Called when main activity is stopped. Clean up any resources.
     * To use again requires re-initialization.
     */
    override fun stop() {
        jointList.clear()
    }

    /**
     * When a new log observer is registered, send a link to this manager.
     * The observer can then initialize its list, if desired. The manager
     * reference should be cleared on "unregister".
     * @param observer
     */
    fun register(observer: GeometryDataObserver) {
        geometryObservers[observer.name] = observer
        observer.reset(jointList)
    }

    fun unregister(observer: GeometryDataObserver) {
        for( key in geometryObservers.keys ) {
            if( geometryObservers.get(key)!!.equals(observer) ) {
                geometryObservers.remove(key,observer)
            }
        }
    }


    private fun initializeObservers() {
        for (observer in geometryObservers.values) {
            observer.reset(jointList)
        }
    }

    /**
     * Notify log observers regarding receipt of a new message.
     */
    private fun notifyObservers(msg: GeometryData) {
        Log.i(CLSS, String.format("notifyObservers: %s", msg.message))
        for (observer in geometryObservers.values) {
            observer.update(msg)
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
        jointList     = mutableListOf<GeometryData>()
        geometryObservers        = mutableMapOf<String, GeometryDataObserver>()
    }
}
