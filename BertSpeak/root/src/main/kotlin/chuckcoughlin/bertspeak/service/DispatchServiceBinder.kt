/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.os.*

class DispatchServiceBinder(private val service: DispatchService) : Binder() {
    fun getService(): DispatchService {
        return service
    }
}
