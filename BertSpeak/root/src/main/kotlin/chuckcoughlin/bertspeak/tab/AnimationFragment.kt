/**
 * Copyright 2023-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.graphics.Canvas
import android.os.Bundle
import android.provider.SyncStateContract.Helpers.update
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import chuckcoughlin.bertspeak.common.DispatchConstants
import chuckcoughlin.bertspeak.data.JsonType.LINK_LOCATIONS
import chuckcoughlin.bertspeak.data.LinkLocation
import chuckcoughlin.bertspeak.data.LinkShapeObserver
import chuckcoughlin.bertspeak.data.StatusData
import chuckcoughlin.bertspeak.data.StatusObserver
import chuckcoughlin.bertspeak.databinding.FragmentAnimationBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.DispatchService.Companion.CLSS
import chuckcoughlin.bertspeak.service.ManagerState
import chuckcoughlin.bertspeak.service.ManagerType
import chuckcoughlin.bertspeak.ui.graphics.Side

/**
 * This fragment displays the robot position right/front/left and allows the uesr to
 * interactively move the limbs via touch gestures. It listens for robot reports of
 * position changes.
 */
class AnimationFragment (pos:Int): BasicAssistantFragment(pos), LinkShapeObserver,StatusObserver {
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
        binding.animationLayoutFront.setOnTouchListener(frontPanel)
        binding.animationLayoutLeft.setOnTouchListener(leftPanel)
        binding.animationLayoutRight.setOnTouchListener(rightPanel)

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
        DispatchService.sendJsonRequest(LINK_LOCATIONS)
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
                DispatchService.sendJsonRequest(LINK_LOCATIONS)
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
    override fun updateGraphics(skeleton:List<LinkLocation>) {
        Log.i(name, String.format("updateGraphics %d elements in skeleton",skeleton.size))
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
