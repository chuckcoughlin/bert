/**
 * Copyright 2022-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak

import android.app.Application
import android.os.StrictMode
import android.util.Log
import chuckcoughlin.bertspeak.service.DispatchService
import kotlinx.coroutines.DelicateCoroutinesApi


class App : Application() {
    lateinit var dispatcher: DispatchService
    fun App() {
        StrictMode.enableDefaults()  // Helps with dangling resource detection
    }

    @DelicateCoroutinesApi
    override fun onCreate() {
        super.onCreate()
        Log.i(CLSS,String.format("onCreate"))
        // Start the comprehensive dispatch connection service
        // This must be in place before the fragment
        dispatcher = DispatchService(applicationContext)
        dispatcher.initialize()
        dispatcher.start()
    }

    @DelicateCoroutinesApi
    override fun onTerminate() {
        super.onTerminate()
        dispatcher.stop()
    }

    val CLSS = "App"
}
