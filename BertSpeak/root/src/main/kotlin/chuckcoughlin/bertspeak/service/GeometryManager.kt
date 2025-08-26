/**
 * Copyright 2024-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.graphics.drawable.shapes.Shape
import android.util.Log
import chuckcoughlin.bertspeak.data.DefaultSkeleton
import chuckcoughlin.bertspeak.data.JsonObserver
import chuckcoughlin.bertspeak.data.JsonType
import chuckcoughlin.bertspeak.data.JsonType.LINK_LOCATIONS
import chuckcoughlin.bertspeak.data.LinkLocation
import chuckcoughlin.bertspeak.data.LinkShapeObserver
import chuckcoughlin.bertspeak.service.DispatchService.Companion.CLSS
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * The geometry manager receives link location information from the robot
 * and converts it into a Shape for display by the animation fragment.
 */
class GeometryManager (service:DispatchService): CommunicationManager,JsonObserver {
    override val name: String
    private val dispatcher = service
    override val managerType = ManagerType.GEOMETRY
    override var managerState = ManagerState.OFF
    private val shapeObservers: MutableMap<String, LinkShapeObserver>
    private val skeleton: MutableMap<String,LinkLocation>
    private val gson: Gson
    override fun start() {
        dispatcher.log(CLSS, String.format("start ..."))
        initializeSkeleton()
        DispatchService.registerForJson(this)
    }
    /**
     * Called when main activity is stopped. Clean up any resources.
     * To use again requires re-initialization.
     */
    override fun stop() {
        DispatchService.unregisterForJson(this)
    }

    fun linkLocationByName(name:String):LinkLocation {
        return skeleton[name]!!
    }
    /**
     * Inform robot of requested new position (from
     * touch screen interaction).
     */
    fun updateJointPosition(location:LinkLocation) {
        val json = Gson().toJson(location)
        DispatchService.reportJsonData(JsonType.MOVE_END_EFFECTOR, json)
    }

    // ================ JsonObserver ======================
    override fun resetItem(map: Map<JsonType, String>) {
        val json = map[JsonType.LINK_LOCATIONS]
        dispatcher.log(CLSS, String.format("resetItem: %s",json))
        if( json!=null && !json.isEmpty() ) {
            skeleton.clear()
            val locType = object : TypeToken<List<LinkLocation>>() {}.type
            val list = gson.fromJson<List<LinkLocation>>(json,locType)
            for(loc in list) {
                skeleton[loc.name] = loc
                Log.i(CLSS, String.format("resetItem: SKeleton added %s",loc.locationToText()))
            }
            notifyObservers(skeleton.values.toList())
        }
    }

    override fun updateItem(type: JsonType, json: String) {
        if( type==LINK_LOCATIONS ) {
            dispatcher.log(CLSS, String.format("updateItem: %s",json))
            if( !json.isEmpty() ) {
                skeleton.clear()
                val locType = object : TypeToken<List<LinkLocation>>() {}.type
                val list = gson.fromJson<List<LinkLocation>>(json,locType)
                for(loc in list) {
                    skeleton[loc.name] = loc
                    Log.i(CLSS, String.format("updateItem: Limb is %s",loc.locationToText()))
                }
                notifyObservers(skeleton.values.toList())
            }
        }
    }
    /**
     * We keep a map of views that observe Shape changes.
     * @param observer
     */
    fun registerShapeViewer(observer: LinkShapeObserver) {
        shapeObservers[observer.name] = observer
        observer.updateGraphics(skeleton.values.toList())
    }

    fun unregisterShapeViewer(observer: LinkShapeObserver) {
        for( key in shapeObservers.keys ) {
            if( !observer.equals(shapeObservers.get(key)) ) {
                shapeObservers.remove(key,observer)
                break
            }
        }
    }

    private fun initializeSkeleton() {
        skeleton.clear()
        val locType = object : TypeToken<List<LinkLocation>>() {}.type
        val list = gson.fromJson<List<LinkLocation>>(DefaultSkeleton.SKELETON,locType)
        for(loc in list) {
            skeleton[loc.name] = loc
            dispatcher.log(CLSS, String.format("initializeSkeleton: Limb is %s",loc.locationToText()))
        }
    }
    private fun initializeObservers() {
        for (observer in shapeObservers.values) {
            observer.resetGraphics()
        }
    }

    /**
     * Notify geometry observers regarding receipt of a new message.
     */
    private fun notifyObservers(locations:List<LinkLocation>) {
        for (observer in shapeObservers.values) {
            observer.updateGraphics(locations)
        }
    }

    private val CLSS = "GeometryManager"

    init {
        name = CLSS
        shapeObservers  = mutableMapOf<String, LinkShapeObserver>()
        skeleton = mutableMapOf<String,LinkLocation>()
        gson = Gson()
    }
}
