/**
 * Copyright 2017-2018 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Define a broadcast receiver that is interested only in the state of tieried facilities
 * within the service. The intent must specify both the facility and its current state.
 */
public class FacilityStateReceiver extends BroadcastReceiver implements ObservableReceiver {
    private static final String CLSS = "FacilityStateReceiver";
    private final List<BroadcastObserver> observers;

    public FacilityStateReceiver() {
        observers = new ArrayList<>();
    }

    /**
     * Notify listeners of the state change, plus create a notification
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(CLSS,String.format("onReceive: ....%s",intent.getAction()));
        if(intent.hasCategory(VoiceConstants.RECEIVER_FACILITY_STATE) ) {
            notifyObservers(intent);
            Log.i(CLSS,String.format("onReceive: %s %s ...",intent.getStringExtra(VoiceConstants.KEY_FACILITY_STATE),
                    intent.getStringExtra(VoiceConstants.KEY_TIERED_FACILITY)));
        }
    }
    // ===================== ObservableReceiver =====================
    @Override
    public void register(final BroadcastObserver observer) {
        Log.i(CLSS,"register: got an observer ...");
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void unregister(final BroadcastObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Intent intent) {
        for (final BroadcastObserver observer : observers) {
            observer.broadcastReceived(intent);
        }
    }
}
