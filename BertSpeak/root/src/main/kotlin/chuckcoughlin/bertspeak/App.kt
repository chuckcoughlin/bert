/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak

import android.app.Application
import android.os.StrictMode
import android.util.Log
import chuckcoughlin.bertspeak.service.DispatchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class App : Application() {
    var dispatcherJob : Job
    fun App() {
        StrictMode.enableDefaults()  // Helps with dangling resource detection
    }
    override fun onCreate() {
        super.onCreate()
        Log.i(CLSS,String.format("onCreate"))
        // Start the comprehensive dispatch connection service
        // This must be in place before the fragment
        val service = DispatchService(applicationContext)
        service.initialize()
        dispatcherJob = GlobalScope.launch() {
            withContext(Dispatchers.IO) {
                service.start()
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        dispatcherJob.cancel()
    }

    val CLSS = "App"

    init {
        dispatcherJob = Job()
    }
}
