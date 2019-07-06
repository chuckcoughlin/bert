/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bert.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class DispatchServiceConnection implements ServiceConnection {
    private boolean bound;
    private DispatchService service;

    public DispatchServiceConnection() {
        this.bound = false;
    }

    public DispatchService getService() { return this.service; }
    public boolean isBound() { return this.bound; }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        bound = false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder bndr) {
        DispatchServiceBinder binder = (DispatchServiceBinder) bndr;
        service = binder.getService();
        bound = true;
    }
}
