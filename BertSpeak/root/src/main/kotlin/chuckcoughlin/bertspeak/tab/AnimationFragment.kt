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
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.FixedSizeList
import chuckcoughlin.bertspeak.data.GeometryData
import chuckcoughlin.bertspeak.data.GeometryDataObserver
import chuckcoughlin.bertspeak.data.TextData
import chuckcoughlin.bertspeak.databinding.FragmentAnimationBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.ui.adapter.GeometryDataAdapter
import chuckcoughlin.bertspeak.ui.adapter.TextDataAdapter
import chuckcoughlin.bertspeak.ui.animate.AnimationView

/**
 * This fragment displays the robot position right/front/left and allows the uesr to
 * interactively move the limbs via touch gestures.
 */
class AnimationFragment (pos:Int): BasicAssistantFragment(pos), GeometryDataObserver {

    override val name: String
    private val adapter: GeometryDataAdapter
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
        DispatchService.unregisterForGeometry(this)
    }

    override fun onDestroyView() {
        Log.i(name, "onDestroyView: ...")
        super.onDestroyView()
        DispatchService.unregisterForGeometry(this)
    }


    override fun reset(list:List<GeometryData>) {
        Log.i(name, "reset: message list ...")
        adapter.resetList(list)
        adapter.reportDataSetChanged()
    }

    override fun update(msg: GeometryData) {
        Log.i(name, String.format("update: message = %s", msg.message))
        adapter.insertMessage(msg)
        adapter.reportDataSetChanged()
    }

    val CLSS = "AnimationFragment"

    init {
        name = CLSS
        adapter = GeometryDataAdapter(FixedSizeList<GeometryData>(BertConstants.NUM_JOINTS))
    }


}
