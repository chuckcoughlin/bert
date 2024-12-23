/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import chuckcoughlin.bertspeak.data.GeometryData
import chuckcoughlin.bertspeak.data.JsonData
import chuckcoughlin.bertspeak.data.GeometryDataObserver
import chuckcoughlin.bertspeak.databinding.FragmentAnimationBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.ui.animate.AnimationView

/**
 * This fragment displays the robot position right/front/left and allows the uesr to
 * interactively move the limbs via touch gestures. It listens for robot reports of
 * position changes.
 */
class AnimationFragment (pos:Int): BasicAssistantFragment(pos), GeometryDataObserver {

    override val name: String
    // These properties are only valid between onCreateView and onDestroyView
    private lateinit var leftPanel: AnimationView
    private lateinit var frontPanel:AnimationView
    private lateinit var rightPanel:AnimationView

    // Inflate the view. It holds three AnimationView panels
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        Log.i(name, "onCreateView: ....")
        val binding = FragmentAnimationBinding.inflate(inflater, container, false)
        leftPanel = binding.animationViewLeft
        frontPanel = binding.animationViewFront
        rightPanel = binding.animationViewRight
        var button = binding.animationRefreshButton
        button.setOnClickListener { refreshButtonClicked() }
        return binding.root
    }

    /**
     * Register for position updates
     */
    override fun onStart() {
        super.onStart()
        DispatchService.registerForGeometry(this)
    }


    override fun onStop() {
        super.onStop()
        DispatchService.unregisterForGeometry(this)
    }

    fun refreshButtonClicked() {

    }
    override fun resetGeometry(geom:GeometryData) {
        Log.i(name, "reset: geometry ...")

    }

    override fun updateGeometry(geom: GeometryData) {
        Log.i(name, String.format("update: geometry "))

    }

    val CLSS = "AnimationFragment"

    init {
        name = CLSS
    }


}
