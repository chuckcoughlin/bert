/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.os.Binder

class DispatchServiceBinder(private val service: DispatchService) : Binder() {
    fun getService(): DispatchService {
        return service
    }
}
