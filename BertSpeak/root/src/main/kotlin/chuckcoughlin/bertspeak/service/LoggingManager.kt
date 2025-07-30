/**
 * Copyright 2024-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.graphics.Rect
import android.util.Log
import chuckcoughlin.bertspeak.common.ContourTag
import chuckcoughlin.bertspeak.data.TextObserver
import chuckcoughlin.bertspeak.common.LandmarkTag
import chuckcoughlin.bertspeak.data.FaceDirection
import chuckcoughlin.bertspeak.data.FacialDetails
import chuckcoughlin.bertspeak.data.JsonType
import chuckcoughlin.bertspeak.data.Point2D
import chuckcoughlin.bertspeak.service.DispatchService.Companion.CLSS
import kotlin.math.PI
import com.google.gson.Gson
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.atan2

/**
 * Provide a vehicle for logging from services that run in a background thread.
 */
class LoggingManager (service:DispatchService): CommunicationManager {
    private val dispatcher = service
    override val managerType = ManagerType.LOGGING
    override var managerState = ManagerState.ACTIVE

    override fun start() {
        Log.i("LoggingManager","Started ...")
    }

    override fun stop() {
    }

    /**
     * Write message to the logger
     */
    fun log(CLSS:String,msg:String) {
        Log.i(CLSS, msg)
    }

}
