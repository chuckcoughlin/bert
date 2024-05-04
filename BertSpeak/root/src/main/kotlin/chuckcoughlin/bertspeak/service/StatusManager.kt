package chuckcoughlin.bertspeak.service

import chuckcoughlin.bertspeak.common.DispatchConstants
import chuckcoughlin.bertspeak.data.StatusData
import chuckcoughlin.bertspeak.data.StatusDataObserver

/**
 * The status manager keeps track of the status of the managers within the
 * application. The DispatchService holds a single instance that is made available
 * to UI fragments via the singleton instance.
 */
class StatusManager(service:DispatchService): CommunicationManager {
    val dispatcher = service
    override val managerType = ManagerType.STATUS
    override var managerState = ManagerState.OFF
    private val map: MutableMap<ManagerType, ManagerState>
    private val observers: MutableMap<String, StatusDataObserver>

    override fun start() {
    }
    override fun stop() {
    }

    /**
     * When a new observer is registered, update it
     *  with states of allmanagers.
     * @param observer
     */
    fun register(observer: StatusDataObserver) {
        observers[observer.name] = observer
        val list: MutableList<StatusData> = ArrayList()
        for (type in map.keys) {
            val ddata = makeDispatchData(type)
            list.add(ddata)
        }
        observer.resetStatus(list)
    }

    fun unregister(observer: StatusDataObserver) {
        observers.remove(observer.name)
    }

    private fun makeDispatchData(type: ManagerType): StatusData {
        val ddata = StatusData(DispatchConstants.ACTION_MANAGER_STATE)
        ddata.type= type
        var state = map.get(type)
        if( state==null ) state = ManagerState.NONE
        ddata.state = state
        return ddata
    }

    /**
     * Notify observers of the facility-state change.
     */
    private fun notifyObservers(ddata: StatusData) {
        for (observer in observers.values) {
            observer.updateStatus(ddata)
        }
    }

    /**
     * Update the status of the observed property. If the status has changed
     * notify observers.
     * @param type manager type
     * @param state new state
     */
    fun updateState(type: ManagerType, state: ManagerState) {
        if( !state.equals(map[type]) ) {
            map[type] = state
            val ddata = makeDispatchData(type)
            notifyObservers(ddata)
        }
    }

    val CLSS = "StatusManager"

    /**
     * Constructor :  On start, initialize the state map.
     */
    init {
        map = mutableMapOf<ManagerType,ManagerState>()
        map[ManagerType.ANNUNCIATOR] = ManagerState.OFF
        map[ManagerType.GEOMETRY] = ManagerState.OFF
        map[ManagerType.SOCKET] = ManagerState.OFF
        map[ManagerType.SPEECH] = ManagerState.OFF
        map[ManagerType.STATUS] = ManagerState.ACTIVE
        map[ManagerType.TEXT] = ManagerState.OFF
        observers = mutableMapOf<String,StatusDataObserver>()
    }
}
