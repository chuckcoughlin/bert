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
import chuckcoughlin.bertspeak.data.GeometryDataObserver
import chuckcoughlin.bertspeak.databinding.FragmentAnimationBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.ui.animate.AnimationView

/**
 * This fragment displays the robot position right/front/left and allows the uesr to
 * interactively move the limbs via touch gestures.
 */
class AnimationFragment (pos:Int): BasicAssistantFragment(pos), GeometryDataObserver {
    val CLSS = "AnimationFragment"
    override val name = CLSS

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
    }

    override fun onDestroyView() {
        Log.i(name, "onDestroyView: ...")
        super.onDestroyView()
        DispatchService.unregisterForGeometry(this)
    }


    override fun reset(list: List<GeometryData>) {

    }

    override fun update(geom: GeometryData) {


    }




}
