/**
 * Copyright 2024-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.util.Log
import chuckcoughlin.bert.common.solver.JointTree
import chuckcoughlin.bertspeak.data.DefaultSkeleton
import chuckcoughlin.bertspeak.data.JsonObserver
import chuckcoughlin.bertspeak.data.JsonType
import chuckcoughlin.bertspeak.data.JointPosition
import chuckcoughlin.bertspeak.data.LinkShapeObserver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * The geometry manager receives link position information from the robot
 * and converts it into a Shape for display by the animation fragment.
 */
class GeometryManager (service:DispatchService): CommunicationManager,JsonObserver {
    override val name: String
    private val dispatcher = service
    override val managerType = ManagerType.GEOMETRY
    override var managerState = ManagerState.OFF
    private val shapeObservers: MutableMap<String, LinkShapeObserver>
    private val skeleton: JointTree
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

    // NOte: This works for appendages also
    fun jointPositionByName(name:String):JointPosition {
        return skeleton.getPositionByName(name)
    }
    /**
     * Inform robot of requested new position (from
     * touch screen interaction).
     */
    fun updateJointPosition(pos:JointPosition) {
        val json = Gson().toJson(pos)
        DispatchService.reportJsonData(JsonType.MOVE_LIMB, json)
    }

    // ================ JsonObserver ======================
    override fun resetItem(map: Map<JsonType, String>) {
        val json = map[JsonType.LINK_POSITIONS]
        dispatcher.log(CLSS, String.format("resetItem: %s",json))
        if( json!=null && !json.isEmpty() ) {
            skeleton.clear()
            val locType = object : TypeToken<List<JointPosition>>() {}.type
            val list = gson.fromJson<List<JointPosition>>(json,locType)
            for(jp in list) {
                skeleton.addJointPosition(jp)
                Log.i(CLSS, String.format("resetItem: SKeleton added %s",jp.positionToText()))
            }
            notifyObservers(skeleton)
        }
    }

    override fun updateItem(type: JsonType, json: String) {
        if( type==JsonType.LINK_POSITIONS ) {
            dispatcher.log(CLSS, String.format("updateItem: %s",json))
            if( !json.isEmpty() ) {
                skeleton.clear()
                val locType = object : TypeToken<List<JointPosition>>() {}.type
                val list = gson.fromJson<List<JointPosition>>(json,locType)
                for(jp in list) {
                    skeleton.addJointPosition(jp)
                    Log.i(CLSS, String.format("updateItem: Limb is %s",jp.positionToText()))
                }
                notifyObservers(skeleton)
            }
        }
    }
    /**
     * We keep a map of views that observe Shape changes.
     * @param observer
     */
    fun registerShapeViewer(observer: LinkShapeObserver) {
        shapeObservers[observer.name] = observer
        observer.updateGraphics(skeleton)
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
        val locType = object : TypeToken<List<JointPosition>>() {}.type
        val list = gson.fromJson<List<JointPosition>>(DefaultSkeleton.SKELETON,locType)
        for(jp in list) {
            skeleton.addJointPosition(jp)
            dispatcher.log(CLSS, String.format("initializeSkeleton: Default is %s",jp.positionToText()))
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
    private fun notifyObservers(tree:JointTree) {
        for (observer in shapeObservers.values) {
            observer.updateGraphics(tree)
        }
    }

    private val CLSS = "GeometryManager"

    init {
        name = CLSS
        shapeObservers  = mutableMapOf<String, LinkShapeObserver>()
        skeleton = JointTree()
        gson = Gson()
    }
}
