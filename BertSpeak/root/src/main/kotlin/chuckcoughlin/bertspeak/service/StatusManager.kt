package chuckcoughlin.bertspeak.service

import android.content.Intent
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.IntentObserver
import java.util.ArrayList
import java.util.HashMap

/**
 * The status manager keeps track of the status of th4 controllers within the
 * application. The DispatchService holds a single instance that is made available
 * to UI fragments through the binder interface.
 */
class StatusManager {
    private val map: MutableMap<ControllerType, ControllerState>
    private val observers: MutableMap<String?, IntentObserver>
    fun getStateForController(fac: ControllerType): ControllerState? {
        return map[fac]
    }

    fun reportState(fac: ControllerType, state: ControllerState) {
        map[fac] = state
        val intent = makeIntent(fac, state)
        notifyObservers(intent)
    }

    /**
     * When a new observer is registered, update with states of all
     * controllers.
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
    fun update(fac: ControllerType, state: ControllerState) {
        map[fac] = state
    }

    private fun makeIntent(fac: ControllerType, state: ControllerState?): Intent {
        val intent = Intent(BertConstants.ACTION_CONTROLLER_STATE)
        intent.addCategory(VoiceConstants.CATEGORY_CONTROLLER_STATE)
        intent.putExtra(VoiceConstants.KEY_CONTROLLER, fac.name)
        intent.putExtra(VoiceConstants.KEY_CONTROLLER_STATE, state!!.name)
        return intent
    }

    /**
     * Notify observers of the facility-state change.
     */
    private fun notifyObservers(intent: Intent) {
        for (observer in observers.values) {
            observer.update(intent)
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
        map[ControllerType.BLUETOOTH] = ControllerState.OFF
        map[ControllerType.SOCKET] = ControllerState.OFF
        map[ControllerType.VOICE] = ControllerState.OFF
        observers = HashMap()
    }
}
