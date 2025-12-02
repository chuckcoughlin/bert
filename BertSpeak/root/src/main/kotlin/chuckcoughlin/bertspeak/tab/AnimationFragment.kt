/**
 * Copyright 2023-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import chuckcoughlin.bertspeak.common.DispatchConstants
import chuckcoughlin.bertspeak.data.JointTree
import chuckcoughlin.bertspeak.data.JsonType
import chuckcoughlin.bertspeak.data.LimbShapeObserver
import chuckcoughlin.bertspeak.data.StatusData
import chuckcoughlin.bertspeak.data.StatusObserver
import chuckcoughlin.bertspeak.databinding.FragmentAnimationBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.ManagerState
import chuckcoughlin.bertspeak.service.ManagerType

/**
 * This fragment displays the robot position right/front/left and allows the uesr to
 * interactively move the limbs via touch gestures. It listens for robot reports of
 * position changes.
 */
class AnimationFragment (pos:Int): BasicAssistantFragment(pos), LimbShapeObserver,StatusObserver {
    // These properties are only valid between onCreateView and onDestroyView
    private lateinit var leftPanel: AnimationView
    private lateinit var frontPanel: AnimationView
    private lateinit var rightPanel: AnimationView
    override val name : String

    // Inflate the view. It holds three AnimationView panels
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        Log.i(CLSS, "onCreateView: ....")
        val binding = FragmentAnimationBinding.inflate(inflater, container, false)
        leftPanel = binding.animationViewLeft
        frontPanel = binding.animationViewFront
        rightPanel = binding.animationViewRight
        frontPanel.setOnTouchListener(frontPanel)
        leftPanel.setOnTouchListener(leftPanel)
        rightPanel.setOnTouchListener(rightPanel)

        var button = binding.animationRefreshButton
        button.setOnClickListener { refreshButtonClicked() }
        return binding.root
    }

    /**
     * Register for position updates
     */
    override fun onStart() {
        super.onStart()
        DispatchService.registerForStatus(this)
        DispatchService.registerForShapes(this)
    }


    override fun onStop() {
        super.onStop()
        DispatchService.unregisterForStatus(this)
        DispatchService.unregisterForShapes(this)
    }

    fun refreshButtonClicked() {
        DispatchService.sendJsonRequest(JsonType.JOINT_COORDINATES)
    }

    // ===================== StatusDataObserver =====================
    override fun resetStatus(list: List<StatusData>) {
        for (ddata in list) {
            updateStatus(ddata)
        }
    }

    /**
     * When the SocketManager comes online, request a link location update.
     */
    override fun updateStatus(data: StatusData) {
        Log.i(name, String.format("updateStatus (%s):%s = %s", data.action, data.type, data.state))
        if (data.action.equals(DispatchConstants.ACTION_MANAGER_STATE)) {
            if (data.type == ManagerType.SOCKET && data.state == ManagerState.ACTIVE) {
                DispatchService.sendJsonRequest(JsonType.JOINT_COORDINATES)
            }
        }
    }

    // ===================== LinkShapeObserver =====================
    override fun resetGraphics() {
        Log.i(name, String.format("resetGraphics: no action"))
        leftPanel.clear()
        frontPanel.clear()
        rightPanel.clear()
    }

    /*
     * Update the skeleton in each of the three panels
     */
    override fun updateGraphics(skeleton: JointTree) {
        Log.i(name, String.format("updateGraphics %d elements in skeleton",skeleton.map.size))
        leftPanel.updateDrawables(skeleton)
        frontPanel.updateDrawables(skeleton)
        rightPanel.updateDrawables(skeleton)

        requireActivity().runOnUiThread {
            frontPanel.invalidate()
            leftPanel.invalidate()
            rightPanel.invalidate()
        }
    }

    val CLSS = "AnimationFragment"

    init {
        name = CLSS
    }
}
