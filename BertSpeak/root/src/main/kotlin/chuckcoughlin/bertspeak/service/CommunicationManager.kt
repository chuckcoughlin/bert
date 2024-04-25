/**
 * Copyright 2023-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

/**
 * Managers are sub-services started by the DispatchService.
 * They may run in either the main UI thread or a background thread.
 * The DispatcherService is passed in the constructor
 */
interface CommunicationManager {
    val managerType: ManagerType   //
    var managerState: ManagerState

    fun start()

    fun stop()
}
