package chuckcoughlin.bert.service;


import android.content.Intent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chuckcoughlin.bert.common.BertConstants;
import chuckcoughlin.bert.common.IntentObserver;

/**
 * The status manager keeps track of the status of tiered facilities within the
 * Voice service. The DispatchService holds a single instance that is made available
 * to UI fragments through the binder interface.
 */
public class StatusManager {
    private final static String CLSS = "StatusManager";
    private final Map<TieredFacility,FacilityState> map;
    private final List<IntentObserver> observers;

    /**
     * Constructor :  On start, initialize the state map.
     */
    public StatusManager() {
        map = new HashMap<>();
        map.put(TieredFacility.BLUETOOTH,FacilityState.IDLE);
        map.put(TieredFacility.SOCKET,FacilityState.IDLE);
        map.put(TieredFacility.VOICE,FacilityState.IDLE);
        this.observers = new ArrayList<>();
    }


    public FacilityState getStateForFacility(TieredFacility fac) { return map.get(fac); }

    public void reportState(TieredFacility fac, FacilityState state) {
        map.put(fac,state);
        Intent intent = makeIntent(fac,state);
        notifyObservers(intent);
    }

    /**
     * When a new observer is registered, update with states of all
     * tiered facilities.
     * @param observer
     */
    public void register(IntentObserver observer) {
        observers.add(observer);
        List<Intent> list = new ArrayList<>();
        for(TieredFacility fac:map.keySet()) {
            list.add(makeIntent(fac,map.get(fac)));
        }
        observer.initialize(list);
    }
    public void unregister(IntentObserver observer) {
        observers.remove(observer);
    }

    public void stop() {
        observers.clear();
    }

    /**
     * Update the status for a facility.
     * @param fac tiered facility
     * @param state new facility state
     */
    public void update(TieredFacility fac,FacilityState state) {
        map.put(fac,state);
    }

    private Intent makeIntent(TieredFacility fac, FacilityState state) {
        Intent intent = new Intent(BertConstants.ACTION_FACILITY_STATE);
        intent.addCategory(VoiceConstants.CATEGORY_FACILITY_STATE);
        intent.putExtra(VoiceConstants.KEY_TIERED_FACILITY,fac.name());
        intent.putExtra(VoiceConstants.KEY_FACILITY_STATE,state.name());
        return intent;
    }

    /**
     * Notify observers of the facility-state change.
     */
    private void notifyObservers( Intent intent) {
        for(IntentObserver observer:observers) {
            if( observer!=null ) {
                observer.update(intent);
            }
        }
    }
}
