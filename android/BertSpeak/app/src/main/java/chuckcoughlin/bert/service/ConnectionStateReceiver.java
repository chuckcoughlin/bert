/**
 * Copyright 2017-2018 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Define a broadcast receiver that is interested only in the connection state
 */
public class ConnectionStateReceiver extends BroadcastReceiver implements ObservableReceiver {
    private final List<BroadcastObserver> observers;
    private ConnectionState currentState;

    public ConnectionStateReceiver() {
        observers = new ArrayList<>();
        currentState = ConnectionState.NONE;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equalsIgnoreCase(VoiceConstants.RECEIVER_SERVICE_STATE) ) {
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
