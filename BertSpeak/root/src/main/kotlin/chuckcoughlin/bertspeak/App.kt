/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak

import android.app.Application
import android.content.Intent
import android.os.StrictMode
import android.util.Log
import chuckcoughlin.bertspeak.common.DispatchConstants
import chuckcoughlin.bertspeak.service.DispatchService


class App : Application() {
    fun App() {
        StrictMode.enableDefaults()  // Helps with dangling resource detection
    }
    override fun onCreate() {
        super.onCreate()
        Log.i(CLSS,String.format("onCreate"))
        // Start the comprehensive dispatch connection service
        // This must be in place before the fragments
        val intent = Intent(this, DispatchService::class.java)
        intent.action = DispatchConstants.ACTION_START_SERVICE
        startService(intent)
    }

    override fun onTerminate() {
        super.onTerminate()
        val intent = Intent(this,DispatchService::class.java)
        intent.action = DispatchConstants.ACTION_STOP_SERVICE
        stopService(intent)
    }

    companion object {
        private const val CLSS = "App"
    }
}
