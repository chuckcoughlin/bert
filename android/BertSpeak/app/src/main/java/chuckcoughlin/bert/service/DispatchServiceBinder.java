/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bert.service;

import android.os.Binder;

public class DispatchServiceBinder extends Binder {
    private DispatchService service;


    public DispatchServiceBinder(DispatchService s) {
        this.service = s;
    }
    public DispatchService getService() {
        return service;
    }

}
