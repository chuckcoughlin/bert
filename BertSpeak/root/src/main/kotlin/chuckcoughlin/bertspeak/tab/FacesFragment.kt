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
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import chuckcoughlin.bertspeak.databinding.FragmentFacesBinding
import chuckcoughlin.bertspeak.ui.facerec.FacialRecognitionView


/**
 * This fragment presents shows the front camera output and attempts to identify the primary face
 * association it with "the operator".
 */
class FacesFragment (pos:Int): BasicAssistantFragment(pos), OnSeekBarChangeListener {
    private val name: String

    // This property is only valid between onCreateView and onDestroyView
    private lateinit var facialRecognitionView: FacialRecognitionView

    // Inflate the view. It holds a fixed image of the robot
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.i(name, "onCreateView: ....")
        val binding = FragmentFacesBinding.inflate(inflater, container, false)
        facialRecognitionView = binding.facerecView
        return binding.root
    }


    /**
     *
     */
    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }


   // ================== ?????Listener ===============

    // =================================OnSeekBarChangeListener =========================
    override fun onStopTrackingTouch(seekBar: SeekBar?) {}


    override fun onStartTrackingTouch(seekBar: SeekBar?) {}


    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        //sliderText.setText("" + progress)
    }

    val CLSS = "FacesFragment"
    val CAPTURE_SIZE = 256

    init {
        name = CLSS
    }
}
