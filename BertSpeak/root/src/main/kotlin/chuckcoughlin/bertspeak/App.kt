/**
 * Copyright 2022-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak

import android.app.Application
import android.os.StrictMode
import android.util.Log
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.db.DatabaseManager
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
        // If we absolutely have to start over again with the database ...
        deleteDatabase(BertConstants.DB_NAME)
        // Initialize the database
        DatabaseManager.initialize()
        // Start the comprehensive dispatch connection service
        // This must be in place before the fragment
        dispatcher = DispatchService(applicationContext)
        dispatcher.initialize()
        dispatcher.start()
        Log.i(CLSS,String.format("onCreate. dispatcher started. complete"))
    }

    @DelicateCoroutinesApi
    override fun onTerminate() {
        super.onTerminate()
        dispatcher.stop()
    }

    val CLSS = "App"
}
