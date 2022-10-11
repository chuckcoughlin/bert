package chuckcoughlin.bertspeak.service

import android.content.Intent
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.IntentObserver
import java.util.ArrayList
import java.util.HashMap

/**
 * The status manager keeps track of the status of tiered facilities within the
 * Voice service. The DispatchService holds a single instance that is made available
 * to UI fragments through the binder interface.
 */
class StatusManager {
    private val map: MutableMap<TieredFacility, FacilityState>
    private val observers: MutableMap<String?, IntentObserver>
    fun getStateForFacility(fac: TieredFacility): FacilityState? {
        return map[fac]
    }

    fun reportState(fac: TieredFacility, state: FacilityState) {
        map[fac] = state
        val intent = makeIntent(fac, state)
        notifyObservers(intent)
    }

    /**
     * When a new observer is registered, update with states of all
     * tiered facilities.
     * @param observer
     */
    fun register(observer: IntentObserver) {
        observers[observer.name] = observer
        val list: MutableList<Intent> = ArrayList()
        for (fac in map.keys) {
            list.add(makeIntent(fac, map[fac]))
        }
        observer.initialize(list)
    }

    fun unregister(observer: IntentObserver) {
        observers.remove(observer.name)
    }

    fun stop() {
        observers.clear()
    }

    /**
     * Update the status for a facility.
     * @param fac tiered facility
     * @param state new facility state
     */
    fun update(fac: TieredFacility, state: FacilityState) {
        map[fac] = state
    }

    private fun makeIntent(fac: TieredFacility, state: FacilityState?): Intent {
        val intent = Intent(BertConstants.ACTION_FACILITY_STATE)
        intent.addCategory(VoiceConstants.CATEGORY_FACILITY_STATE)
        intent.putExtra(VoiceConstants.KEY_TIERED_FACILITY, fac.name)
        intent.putExtra(VoiceConstants.KEY_FACILITY_STATE, state!!.name)
        return intent
    }

    /**
     * Notify observers of the facility-state change.
     */
    private fun notifyObservers(intent: Intent) {
        for (observer in observers.values) {
            observer?.update(intent)
        }
    }

    companion object {
        private const val CLSS = "StatusManager"
    }

    /**
     * Constructor :  On start, initialize the state map.
     */
    init {
        map = HashMap()
        map[TieredFacility.BLUETOOTH] = FacilityState.IDLE
        map[TieredFacility.SOCKET] = FacilityState.IDLE
        map[TieredFacility.VOICE] = FacilityState.IDLE
        observers = HashMap()
    }
}
