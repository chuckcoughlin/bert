/**
 * Copyright 2017-2018 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

/**
 * Define a broadcast receiver that is interested only in the state of ordered actions
 * within the service. The intent must specify both the action and its current state.
 */
public class ActionStateReceiver extends BroadcastReceiver implements ObservableReceiver {
    private final List<BroadcastObserver> observers;

    public ActionStateReceiver() {
        observers = new ArrayList<>();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.hasCategory(VoiceConstants.RECEIVER_SERVICE_STATE) ) {
            notifyObservers(intent);
        }
    }
    // ===================== ObservableReceiver =====================
    @Override
    public void register(final BroadcastObserver observer) {
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
