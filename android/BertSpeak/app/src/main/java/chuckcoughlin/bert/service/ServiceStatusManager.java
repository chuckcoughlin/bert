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
 * Voice service. The singleton instance is created and shutdown in the MainActivity.
 */
public class ServiceStatusManager {
    private final static String CLSS = "BertLogManager";
    private static volatile ServiceStatusManager instance = null;
    private final Map<TieredFacility,FacilityState> map;
    private final List<IntentObserver> observers;

    /**
     * Constructor is private per Singleton pattern. This forces use of the single instance.
     * On start, initialize the state map.
     */
    private ServiceStatusManager() {
        map = new HashMap<>();
        map.put(TieredFacility.BLUETOOTH,FacilityState.IDLE);
        map.put(TieredFacility.SOCKET,FacilityState.IDLE);
        map.put(TieredFacility.VOICE,FacilityState.IDLE);
        observers = new ArrayList<>();
    }

    /**
     * Use this method in the initial activity. We need to assign the context.
     * @return the Singleton instance
     */
    public static synchronized ServiceStatusManager initialize() {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        if (instance == null) {
            instance = new ServiceStatusManager();
        }
        else {
            android.util.Log.w(CLSS,String.format("initialize: Status manager exists, re-initialization ignored"));
        }
        return instance;
    }

    /**
     * Called when main activity is destroyed. Clean up any resources.
     * To use again requires re-initialization.
     */
    public static void stop() {
        if (instance != null) {
            synchronized (ServiceStatusManager.class) {
                instance = null;
            }
        }
    }
    /**
     * Use this method for all subsequent calls. We often don't have
     * a convenient context.
     * @return the Singleton instance.
     */
    public static synchronized ServiceStatusManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Attempt to return uninitialized copy of ServiceStatusManager");
        }
        return instance;
    }

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
